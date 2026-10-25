package assos.hytale.home.commands;

import assos.hytale.home.home.Home;
import assos.hytale.home.home.HomeService;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.List;

public final class HomeSuggestions {

    private HomeSuggestions() {
    }

    public static void suggest(@Nonnull HomeService homeService, @Nonnull CommandSender sender,
            @Nonnull String input, @Nonnull SuggestionResult result) {
        if (!(sender instanceof PlayerRef playerRef)) {
            return;
        }
        List<String> names = homeService.list(playerRef.getUuid()).stream().map(Home::getName).toList();
        SuggestionUtil.suggestFiltered(names, input, result);
    }
}
