package assos.hytale.home;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;

public final class HomePermissions {

    public static final String HOME = "hytale.homes.home";
    public static final String SETHOME = "hytale.homes.sethome";
    public static final String HOMES = "hytale.homes.homes";
    public static final String DELHOME = "hytale.homes.delhome";
    public static final String BYPASS_COOLDOWN = "hytale.homes.bypass.cooldown";

    private HomePermissions() {
    }

    public static void apply(AbstractCommand command, boolean requirePermissions, String permission) {
        if (requirePermissions) {
            command.requirePermission(permission);
        } else {
            command.requireNoPermission();
        }
    }
}
