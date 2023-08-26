package dev.lemonclient.addon.modules.misc;

import dev.lemonclient.addon.LemonClient;
import dev.lemonclient.addon.LemonModule;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

public class SoundModifier extends LemonModule {
    public SoundModifier() {
        super(LemonClient.Misc, "Sound Modifier", "Modifies sounds to make crystal pvp less horrible for ears.");
    }

    private final SettingGroup sgCrystal = settings.createGroup("Crystal");

    public final Setting<Boolean> crystalHits = sgCrystal.add(new BoolSetting.Builder()
        .name("Crystal Hit Sound")
        .description("Allows hit sounds when attacking end crystal.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> crystalHitVolume = sgCrystal.add(new DoubleSetting.Builder()
        .name("Crystal Hit Volume")
        .description("Multiplies crystal hit volumes.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(crystalHits::get)
        .build()
    );
    public final Setting<Double> crystalHitPitch = sgCrystal.add(new DoubleSetting.Builder()
        .name("Crystal Hit Pitch")
        .description("Multiplies pitch of crystal hit sounds.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(crystalHits::get)
        .build()
    );
    public final Setting<Boolean> expSound = sgCrystal.add(new BoolSetting.Builder()
        .name("Explosion Sound")
        .description("Allows explosion sounds")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> explosionVolume = sgCrystal.add(new DoubleSetting.Builder()
        .name("Explosion Volume")
        .description("Multiplies explosion volumes.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(expSound::get)
        .build()
    );
    public final Setting<Double> explosionPitch = sgCrystal.add(new DoubleSetting.Builder()
        .name("Explosion Pitch")
        .description("Multiplies pitch of explosions sounds.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(expSound::get)
        .build()
    );
}
