package assos.hytale.home.commands;

import assos.hytale.home.home.HomeService;
import assos.hytale.home.ui.HomesPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class HomesUiCommand extends AbstractPlayerCommand {

    private final HomeService homeService;

    public HomesUiCommand(@Nonnull HomeService homeService) {
        super("homesui", "Open the homes interface");
        requireNoPermission();
        this.homeService = homeService;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Could not open the homes interface.").color("#FF5555"));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new HomesPage(playerRef, homeService));
    }
}
