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
            .append(new KeyedCodec<>("RequirePermissions", Codec.BOOLEAN),
                    (config, value, info) -> config.requirePermissions = value,
                    (config, info) -> config.requirePermissions)
            .add()
            .append(new KeyedCodec<>("TeleportCooldownSeconds", Codec.INTEGER),
                    (config, value, info) -> config.teleportCooldownSeconds = value,
                    (config, info) -> config.teleportCooldownSeconds)
            .add()
            .append(new KeyedCodec<>("TeleportWarmupSeconds", Codec.INTEGER),
                    (config, value, info) -> config.teleportWarmupSeconds = value,
                    (config, info) -> config.teleportWarmupSeconds)
            .add()
            .append(new KeyedCodec<>("CombatTagEnabled", Codec.BOOLEAN),
                    (config, value, info) -> config.combatTagEnabled = value,
                    (config, info) -> config.combatTagEnabled)
            .add()
            .append(new KeyedCodec<>("CombatTagSeconds", Codec.INTEGER),
                    (config, value, info) -> config.combatTagSeconds = value,
                    (config, info) -> config.combatTagSeconds)
            .add()
            .append(new KeyedCodec<>("SafeLanding", Codec.BOOLEAN),
                    (config, value, info) -> config.safeLanding = value,
                    (config, info) -> config.safeLanding)
            .add()
            .append(new KeyedCodec<>("BlockedWorlds", Codec.STRING),
                    (config, value, info) -> config.blockedWorlds = value,
                    (config, info) -> config.blockedWorlds)
            .add()
            .build();

    private int maxHomes = 5;
    private String defaultHomeName = "home";
    private boolean allowCrossWorld = true;
    private int maxHomeNameLength = 24;
    private boolean requirePermissions;
    private int teleportCooldownSeconds = 30;
    private int teleportWarmupSeconds = 3;
    private boolean combatTagEnabled;
    private int combatTagSeconds = 10;
    private boolean safeLanding = true;
    private String blockedWorlds = "";

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

    public boolean isRequirePermissions() {
        return requirePermissions;
    }

    public int getTeleportCooldownSeconds() {
        return teleportCooldownSeconds;
    }

    public int getTeleportWarmupSeconds() {
        return teleportWarmupSeconds;
    }

    public boolean isCombatTagEnabled() {
        return combatTagEnabled;
    }

    public int getCombatTagSeconds() {
        return combatTagSeconds;
    }

    public boolean isSafeLanding() {
        return safeLanding;
    }

    public String getBlockedWorlds() {
        return blockedWorlds;
    }
}
