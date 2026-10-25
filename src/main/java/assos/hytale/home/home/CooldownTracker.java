package assos.hytale.home.home;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownTracker {

    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public long remainingMillis(UUID playerId, long nowMillis, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return 0;
        }
        Long last = lastUse.get(playerId);
        if (last == null) {
            return 0;
        }
        long elapsed = nowMillis - last;
        return elapsed >= cooldownMillis ? 0 : cooldownMillis - elapsed;
    }

    public void mark(UUID playerId, long nowMillis) {
        lastUse.put(playerId, nowMillis);
    }

    public void clear(UUID playerId) {
        lastUse.remove(playerId);
    }
}
