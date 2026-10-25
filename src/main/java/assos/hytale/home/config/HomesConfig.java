package assos.hytale.home.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class HomesConfig {

    public static final BuilderCodec<HomesConfig> CODEC = BuilderCodec.builder(HomesConfig.class, HomesConfig::new)
            .append(new KeyedCodec<>("MaxHomes", Codec.INTEGER),
                    (config, value, info) -> config.maxHomes = value,
                    (config, info) -> config.maxHomes)
            .add()
            .append(new KeyedCodec<>("DefaultHomeName", Codec.STRING),
                    (config, value, info) -> config.defaultHomeName = value,
                    (config, info) -> config.defaultHomeName)
            .add()
            .append(new KeyedCodec<>("AllowCrossWorld", Codec.BOOLEAN),
                    (config, value, info) -> config.allowCrossWorld = value,
                    (config, info) -> config.allowCrossWorld)
            .add()
            .append(new KeyedCodec<>("MaxHomeNameLength", Codec.INTEGER),
                    (config, value, info) -> config.maxHomeNameLength = value,
                    (config, info) -> config.maxHomeNameLength)
            .add()
            .build();

    private int maxHomes = 5;
    private String defaultHomeName = "home";
    private boolean allowCrossWorld = true;
    private int maxHomeNameLength = 24;

    private HomesConfig() {
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public String getDefaultHomeName() {
        return defaultHomeName;
    }

    public boolean isAllowCrossWorld() {
        return allowCrossWorld;
    }

    public int getMaxHomeNameLength() {
        return maxHomeNameLength;
    }
}
