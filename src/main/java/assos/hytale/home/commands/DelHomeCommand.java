package assos.hytale.home.commands;

import assos.hytale.home.HomePermissions;
import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

public final class DelHomeCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final HomeService homeService;

    public DelHomeCommand(@Nonnull HomeService homeService, boolean requirePermissions) {
        super("delhome", "Delete a home");
        HomePermissions.apply(this, requirePermissions, HomePermissions.DELHOME);
        this.homeService = homeService;
        addUsageVariant(new NamedDelHomeCommand(homeService, requirePermissions));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        deleteHome(context, playerRef, homeService, homeService.getDefaultHomeName());
    }

    private static void deleteHome(@Nonnull CommandContext context, @Nonnull PlayerRef playerRef,
            @Nonnull HomeService homeService, @Nonnull String rawName) {
        Optional<Home> target;
        try {
            target = homeService.resolveSelector(playerRef.getUuid(), rawName);
        } catch (IllegalArgumentException exception) {
            context.sendMessage(Message.raw(exception.getMessage()).color("#FF5555"));
            return;
        }

        if (target.isEmpty()) {
            String label = isNumeric(rawName) ? "Home number " + rawName : "Home '" + rawName + "'";
            context.sendMessage(
                    Message.raw(label + " not found. Use /homes to see the numbered list.").color("#FF5555"));
            return;
        }

        String name = target.get().getName();
        try {
            boolean removed = homeService.delete(playerRef.getUuid(), name).isPresent();
            if (removed) {
                context.sendMessage(Message.raw("Home '" + name + "' deleted.").color("#55FF55"));
            } else {
                context.sendMessage(Message.raw("Home '" + name + "' not found.").color("#FF5555"));
            }
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to delete home %s for %s", name, playerRef.getUsername());
            context.sendMessage(Message.raw("Could not save your homes. Check the server logs.").color("#FF5555"));
        }
    }

    private static boolean isNumeric(@Nonnull String value) {
        return value.trim().matches("\\d+");
    }

    private static final class NamedDelHomeCommand extends AbstractPlayerCommand {

        private final HomeService homeService;
        private final RequiredArg<String> nameArg;

        private NamedDelHomeCommand(@Nonnull HomeService homeService, boolean requirePermissions) {
            super("Delete a named home");
            HomePermissions.apply(this, requirePermissions, HomePermissions.DELHOME);
            this.homeService = homeService;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
            this.nameArg.suggest((sender, input, offset, result) -> HomeSuggestions.suggest(homeService, sender,
                    input, result));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            deleteHome(context, playerRef, homeService, nameArg.get(context));
        }
    }
}
