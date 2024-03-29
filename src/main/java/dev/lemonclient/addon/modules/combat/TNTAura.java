package dev.lemonclient.addon.modules.combat;

import dev.lemonclient.addon.LemonClient;
import dev.lemonclient.addon.LemonModule;
import dev.lemonclient.addon.enums.RotationType;
import dev.lemonclient.addon.enums.SwingHand;
import dev.lemonclient.addon.managers.Managers;
import dev.lemonclient.addon.utils.SettingUtils;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static dev.lemonclient.addon.utils.entity.EntityInfo.getBlockPos;
import static dev.lemonclient.addon.utils.entity.EntityInfo.isSurrounded;
import static dev.lemonclient.addon.utils.world.BlockInfo.getBlock;
import static dev.lemonclient.addon.utils.world.BlockInfo.isAir;

public class TNTAura extends LemonModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoBreak = settings.createGroup("Auto-Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgSwing = settings.createGroup("Swing");
    private final SettingGroup sgObsidianRender = settings.createGroup("Obsidian-Render");
    private final SettingGroup sgTNTRender = settings.createGroup("TNT-Render");
    private final SettingGroup sgBreakRender = settings.createGroup("Break-Render");

    // General
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("target-range").description("max range to target").defaultValue(4).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("place-delay").description("How many ticks between obsidian placement").defaultValue(1).build());
    // Auto Break
    private final Setting<Boolean> autoBreak = sgAutoBreak.add(new BoolSetting.Builder().name("auto-break").description("attemps to auto break").defaultValue(false).build());
    public final Setting<mineMode> breakMode = sgAutoBreak.add(new EnumSetting.Builder<mineMode>().name("break-mode").defaultValue(mineMode.Normal).visible(autoBreak::get).build());
    // Pause
    private final Setting<Boolean> burrowPause = sgPause.add(new BoolSetting.Builder().name("pause-on-burrow").description("will pause if enemy is burrowed").defaultValue(true).build());
    private final Setting<Boolean> antiSelf = sgPause.add(new BoolSetting.Builder().name("anti-self").description("pause if enemy in your hole").defaultValue(true).build());
    private final Setting<Boolean> holePause = sgPause.add(new BoolSetting.Builder().name("only-in-hole").description("pause if enemy isnt in hole").defaultValue(false).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());
    // Swing
    private final Setting<Boolean> swing = sgSwing.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Renders your swing client-side.")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwingHand> placeHand = sgSwing.add(new EnumSetting.Builder<SwingHand>()
        .name("Swing Hand")
        .description("Which hand should be swung.")
        .defaultValue(SwingHand.RealHand)
        .visible(swing::get)
        .build()
    );
    // Obsidian render
    private final Setting<Boolean> obsidianRender = sgObsidianRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> obsidianShapeMode = sgObsidianRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> obsidianSideColor = sgObsidianRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(0, 255, 0, 60)).build());
    private final Setting<SettingColor> obsidianLineColor = sgObsidianRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(0, 255, 0, 190)).build());
    private final Setting<SettingColor> obsidianNextSideColor = sgObsidianRender.add(new ColorSetting.Builder().name("next-side-color").description("The side color of the next block to be placed.").defaultValue(new SettingColor(255, 0, 0, 60)).build());
    private final Setting<SettingColor> obsidianNextLineColor = sgObsidianRender.add(new ColorSetting.Builder().name("next-line-color").description("The line color of the next block to be placed.").defaultValue(new SettingColor(255, 0, 0, 190)).build());
    //tnt render
    private final Setting<Boolean> tntRender = sgTNTRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> tntShapeMode = sgTNTRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> tntSideColor = sgTNTRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(0, 255, 0, 60)).build());
    private final Setting<SettingColor> tntLineColor = sgTNTRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(0, 255, 0, 190)).build());
    //mine render
    private final Setting<Boolean> breakRender = sgBreakRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> breakShapeMode = sgBreakRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> breakSideColor = sgBreakRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(0, 0, 255, 60)).build());
    private final Setting<SettingColor> breakLineColor = sgBreakRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(0, 0, 255, 190)).build());

    private PlayerEntity target;
    private final List<BlockPos> obsidianPos = new ArrayList<>();
    private int ticks;
    private Direction direction;
    private boolean rofl;
    private boolean toggled;

    public TNTAura() {
        super(LemonClient.Combat, "TNT Aura", "Placing & igniting TNT around enemy");
    }

    @Override
    public void onActivate() {
        obsidianPos.clear();
        ticks = 0;
        rofl = false;
        toggled = false;
    }

    @Override
    public void onDeactivate() {
        obsidianPos.clear();
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        direction = event.direction;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        //auto dissable
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (!obsidian.isHotbar() && !toggled) {
            obsidianPos.clear();
            sendDisableMsg("No obsidian found in hotbar");
            toggle();
            toggled = true;
        }

        FindItemResult flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);

        if (!flint.isHotbar() && !toggled) {
            obsidianPos.clear();
            sendDisableMsg("No flint and steel found in hotbar");
            toggle();
            toggled = true;
        }

        FindItemResult tnt = InvUtils.findInHotbar(Items.TNT);

        if (!tnt.isHotbar() && !toggled) {
            obsidianPos.clear();
            sendDisableMsg("No TNT found in hotbar");
            toggle();
            toggled = true;
        }

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

        if (!pickaxe.isHotbar() && !toggled) {
            obsidianPos.clear();
            sendDisableMsg("No pickaxe found in hotbar");
            toggle();
            toggled = true;
        }

        if (TargetUtils.isBadTarget(target, range.get())) {
            target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        }
        if (target == null) return;

        if (burrowPause.get() && isBurrowed(target) && !toggled) {
            obsidianPos.clear();
            ChatUtils.error("Target is burrowed");
            toggle();
            toggled = true;
        }

        if (antiSelf.get() && antiSelf(target) && !toggled) {
            obsidianPos.clear();
            ChatUtils.error("Target in your hole!");
            toggle();
            toggled = true;
        }

        if (holePause.get() && !isSurrounded(target) && !toggled) {
            obsidianPos.clear();
            ChatUtils.error("Target is not surrounded");
            toggle();
            toggled = true;
        }

        if (TargetUtils.isBadTarget(target, range.get()) && !toggled) {
            ChatUtils.error("Enemy is too far away");
            toggle();
            toggled = true;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        if (allowTNT(target)) {
            placeTNT(target);
            igniteTNT(target.getBlockPos().up(2), flint);
        }

        if (!mineBlockstate(target.getBlockPos().up(2)) && autoBreak.get()) {
            mine(target.getBlockPos().up(2), pickaxe);
        }

        //place obsidian
        placeObsidian(target);

        if (ticks >= delay.get() && obsidianPos.size() > 0) {
            BlockPos blockPos = obsidianPos.get(obsidianPos.size() - 1);

            if (SettingUtils.shouldRotate(RotationType.BlockPlace)) Managers.ROTATION.start(blockPos, priority, RotationType.BlockPlace, Objects.hash(name + "placing"));
            if (BlockUtils.place(blockPos, obsidian, 50, true)) {
                obsidianPos.remove(blockPos);
            }
            ticks = 0;
        } else {
            ticks++;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        //obsidian render
        if (obsidianRender.get() && !obsidianPos.isEmpty()) {
            for (BlockPos pos : obsidianPos) {
                boolean isFirst = pos.equals(obsidianPos.get(obsidianPos.size() - 1));

                Color side = isFirst ? obsidianNextSideColor.get() : obsidianSideColor.get();
                Color line = isFirst ? obsidianNextLineColor.get() : obsidianLineColor.get();

                event.renderer.box(pos, side, line, obsidianShapeMode.get(), 0);
            }
        }

        if (tntRender.get() && target != null && allowTNT(target)) {
            if (tntBlockstate(target.getBlockPos().add(0, 2, 0))) {
                event.renderer.box(target.getBlockPos().add(0, 2, 0), tntSideColor.get(), tntLineColor.get(), tntShapeMode.get(), 0);
            }
        }

        if (breakRender.get() && target != null && autoBreak.get()) {
            if (!mineBlockstate(target.getBlockPos().add(0, 2, 0))) {
                event.renderer.box(target.getBlockPos().add(0, 2, 0), breakSideColor.get(), breakLineColor.get(), breakShapeMode.get(), 0);
            }
        }
    }


    private void placeObsidian(PlayerEntity target) {
        obsidianPos.clear();
        BlockPos targetPos = getBlockPos(target);
        add(targetPos.add(0, 3, 0));
        add(targetPos.add(1, 2, 0));
        add(targetPos.add(-1, 2, 0));
        add(targetPos.add(0, 2, 1));
        add(targetPos.add(0, 2, -1));
    }

    private void placeTNT(PlayerEntity target) {
        FindItemResult tnt = InvUtils.findInHotbar(Items.TNT);
        BlockPos targetPos = getBlockPos(target);
        BlockPos tntPos = targetPos.add(0, 2, 0);
        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) Managers.ROTATION.start(tntPos, priority, RotationType.BlockPlace, Objects.hash(name + "placing"));
        BlockUtils.place(tntPos, tnt, false, 50, true, true);
    }

    private void add(BlockPos blockPos) {
        if (!obsidianPos.contains(blockPos) && BlockUtils.canPlace(blockPos)) obsidianPos.add(blockPos);
    }

    private void igniteTNT(BlockPos pos, FindItemResult item) {
        InvUtils.swap(item.slot(), true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
        InvUtils.swapBack();
    }

    public boolean tntBlockstate(BlockPos Pos) {
        return getBlock(Pos) == Blocks.AIR
            || getBlock(Pos) == Blocks.TNT;
    }

    public boolean allowTNT(LivingEntity target) {
        assert mc.world != null;

        return !isAir(target.getBlockPos().add(1, 2, 0)) && !isAir(target.getBlockPos().add(-1, 2, 0)) && !isAir(target.getBlockPos().add(0, 2, 1)) && !isAir(target.getBlockPos().add(0, 2, -1)) && !isAir(target.getBlockPos().add(0, 3, 0));
    }

    public boolean mineBlockstate(BlockPos Pos) {
        return getBlock(Pos) == Blocks.AIR
            || getBlock(Pos) == Blocks.TNT
            || getBlock(Pos) == Blocks.BEDROCK;

    }

    public void mine(BlockPos blockPos, FindItemResult item) {

        if (breakMode.get() == mineMode.Normal) {
            InvUtils.swap(item.slot(), false);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            if (swing.get()) clientSwing(placeHand.get(), Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }
        if (breakMode.get() == mineMode.Instant) {
            InvUtils.swap(item.slot(), false);

            if (!rofl) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                rofl = true;
            }
            if (SettingUtils.shouldRotate(RotationType.Mining)) {
                Managers.ROTATION.start(blockPos, priority, RotationType.Mining, Objects.hash(name + "mining"));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            }
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    private boolean isBurrowed(LivingEntity target) {
        assert mc.world != null;

        return !isAir(getBlockPos(target));
    }

    private boolean antiSelf(LivingEntity target) {

        if (!(mc.player.getBlockPos().getX() == target.getBlockPos().getX() && mc.player.getBlockPos().getZ() == target.getBlockPos().getZ() && mc.player.getBlockPos().getY() == target.getBlockPos().getY()))
            return false;
        return true;
    }

    public enum mineMode {
        Normal, Instant;
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
