package assos.hytale.home.home;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class HomeStorage implements HomeRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();

    public HomeStorage(Path file) {
        this.file = file;
    }

    @Override
    public synchronized Home get(UUID playerId, String name) {
        Map<String, Home> playerHomes = homes.get(playerId);
        return playerHomes == null ? null : playerHomes.get(name);
    }

    @Override
    public synchronized List<Home> list(UUID playerId) {
        Map<String, Home> playerHomes = homes.get(playerId);
        if (playerHomes == null || playerHomes.isEmpty()) {
            return List.of();
        }
        List<Home> result = new ArrayList<>(playerHomes.values());
        result.sort(Comparator.comparing(Home::getName));
        return result;
    }

    @Override
    public synchronized int count(UUID playerId) {
        Map<String, Home> playerHomes = homes.get(playerId);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    @Override
    public synchronized void put(UUID playerId, Home home) {
        homes.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>()).put(home.getName(), home);
    }

    @Override
    public synchronized boolean remove(UUID playerId, String name) {
        Map<String, Home> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return false;
        }
        Home removed = playerHomes.remove(name);
        if (playerHomes.isEmpty()) {
            homes.remove(playerId);
        }
        return removed != null;
    }

    public synchronized void load() throws IOException {
        if (!Files.exists(file)) {
            homes.clear();
            return;
        }

        Map<UUID, Map<String, Home>> loaded = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StoredHomes stored = GSON.fromJson(reader, StoredHomes.class);
            if (stored != null && stored.players != null) {
                stored.players.forEach((rawId, rawHomes) -> {
                    UUID playerId;
                    try {
                        playerId = UUID.fromString(rawId);
                    } catch (IllegalArgumentException exception) {
                        return;
                    }
                    Map<String, Home> playerHomes = new LinkedHashMap<>();
                    if (rawHomes != null) {
                        rawHomes.forEach((name, home) -> {
                            if (home != null) {
                                home.setName(name);
                                playerHomes.put(name, home);
                            }
                        });
                    }
                    if (!playerHomes.isEmpty()) {
                        loaded.put(playerId, playerHomes);
                    }
                });
            }
        } catch (JsonParseException | IOException exception) {
            throw new IOException("Could not read " + file, exception);
        }

        homes.clear();
        homes.putAll(loaded);
    }

    @Override
    public synchronized void save() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StoredHomes stored = new StoredHomes();
        homes.forEach((playerId, playerHomes) -> stored.players.put(playerId.toString(), new TreeMap<>(playerHomes)));

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(stored, writer);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class StoredHomes {
        private int version = 1;
        private Map<String, Map<String, Home>> players = new TreeMap<>();
    }
}
