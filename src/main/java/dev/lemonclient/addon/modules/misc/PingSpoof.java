package dev.lemonclient.addon.modules.misc;

import dev.lemonclient.addon.LemonClient;
import dev.lemonclient.addon.LemonModule;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class PingSpoof extends LemonModule {
    public PingSpoof() {
        super(LemonClient.Misc, "Ping Spoof", "Modify your ping.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> keepAlive = sgGeneral.add(new BoolSetting.Builder()
        .name("Keep Alive")
        .description("Delays keep alive packets.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> pong = sgGeneral.add(new BoolSetting.Builder()
        .name("Pong")
        .description("Delays pong packets.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("Ping")
        .description("Increases your ping by this much.")
        .defaultValue(69)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );
}
