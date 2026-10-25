package assos.hytale.home.home;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory {@link HomeRepository} for tests.
 *
 * <p>Homes are keyed by player and home name. {@link #list(UUID)} returns the homes sorted by
 * name so tests do not depend on insertion order. Every call to {@link #save()} is counted, and
 * {@link #setFailOnSave(boolean)} makes it throw instead of succeeding.
 */
final class InMemoryHomeRepository implements HomeRepository {

    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();

    private int saveCallCount;
    private boolean failOnSave;

    /**
     * Counts the {@link #save()} invocations, successful or not.
     *
     * @return how many times save was called
     */
    int saveCallCount() {
        return saveCallCount;
    }

    /**
     * Makes every subsequent {@link #save()} throw an {@link IOException}.
     *
     * @param failOnSave true to fail
     */
    void setFailOnSave(boolean failOnSave) {
        this.failOnSave = failOnSave;
    }

    @Override
    public Home get(UUID playerId, String name) {
        Map<String, Home> playerHomes = homes.get(playerId);
        return playerHomes == null ? null : playerHomes.get(name);
    }

    @Override
    public List<Home> list(UUID playerId) {
        Map<String, Home> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return List.of();
        }
        List<Home> sorted = new ArrayList<>(playerHomes.values());
        sorted.sort(Comparator.comparing(Home::getName));
        return sorted;
    }

    @Override
    public int count(UUID playerId) {
        Map<String, Home> playerHomes = homes.get(playerId);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    @Override
    public void put(UUID playerId, Home home) {
        homes.computeIfAbsent(playerId, id -> new HashMap<>()).put(home.getName(), home);
    }

    @Override
    public boolean remove(UUID playerId, String name) {
        Map<String, Home> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return false;
        }
        boolean removed = playerHomes.remove(name) != null;
        if (playerHomes.isEmpty()) {
            homes.remove(playerId);
        }
        return removed;
    }

    @Override
    public void save() throws IOException {
        saveCallCount++;
        if (failOnSave) {
            throw new IOException("simulated save failure");
        }
    }

    /**
     * Builds a home with explicit values.
     */
    static Home home(String name, String world, double x, double y, double z,
                     float pitch, float yaw, float roll, long createdAt) {
        return new Home(name, world, x, y, z, pitch, yaw, roll, createdAt);
    }

    /**
     * Builds a home with fixed, recognizable values.
     *
     * @param name the home name
     * @return a home in "world" at 1.5 / 64.0 / -2.5
     */
    static Home home(String name) {
        return home(name, "world", 1.5, 64.0, -2.5, 5.0f, 90.0f, 1.0f, 1_700_000_000_000L);
    }
}
