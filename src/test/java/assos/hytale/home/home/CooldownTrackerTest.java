package assos.hytale.home.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CooldownTrackerTest {

    private static final long COOLDOWN = 5_000L;

    private final CooldownTracker tracker = new CooldownTracker();

    @Test
    void unknownPlayerHasNoRemainingCooldown() {
        assertEquals(0L, tracker.remainingMillis(UUID.randomUUID(), 1_000L, COOLDOWN));
    }

    @Test
    void remainingDecreasesWithTimeAndReachesZero() {
        UUID player = UUID.randomUUID();
        tracker.mark(player, 1_000L);

        assertEquals(COOLDOWN, tracker.remainingMillis(player, 1_000L, COOLDOWN));
        assertEquals(3_000L, tracker.remainingMillis(player, 3_000L, COOLDOWN));
        assertEquals(1_000L, tracker.remainingMillis(player, 5_000L, COOLDOWN));
        assertEquals(0L, tracker.remainingMillis(player, 6_000L, COOLDOWN));
        assertEquals(0L, tracker.remainingMillis(player, 60_000L, COOLDOWN));
    }

    @Test
    void nonPositiveCooldownHasNoRemainingTime() {
        UUID player = UUID.randomUUID();
        tracker.mark(player, 1_000L);

        assertEquals(0L, tracker.remainingMillis(player, 1_000L, 0L));
        assertEquals(0L, tracker.remainingMillis(player, 1_000L, -5_000L));
    }

    @Test
    void clearRemovesTheCooldown() {
        UUID player = UUID.randomUUID();
        tracker.mark(player, 1_000L);

        tracker.clear(player);

        assertEquals(0L, tracker.remainingMillis(player, 1_000L, COOLDOWN));
    }

    @Test
    void playersAreIndependent() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.mark(first, 1_000L);
        tracker.mark(second, 4_000L);

        assertEquals(3_000L, tracker.remainingMillis(first, 3_000L, COOLDOWN));
        assertEquals(COOLDOWN, tracker.remainingMillis(second, 4_000L, COOLDOWN));

        tracker.clear(first);

        assertEquals(0L, tracker.remainingMillis(first, 4_000L, COOLDOWN));
        assertEquals(COOLDOWN, tracker.remainingMillis(second, 4_000L, COOLDOWN));
    }
}
