package assos.hytale.home.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HomeStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOnMissingFileLeavesTheStoreEmpty() throws Exception {
        HomeStorage storage = new HomeStorage(tempDir.resolve("homes.json"));
        UUID player = UUID.randomUUID();

        storage.load();

        assertEquals(0, storage.count(player));
        assertTrue(storage.list(player).isEmpty());
        assertNull(storage.get(player, "home"));
    }

    @Test
    void saveThenLoadRoundTripsEveryField() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID other = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");
        Home home = InMemoryHomeRepository.home("Cabin", "world_nether", 12.5, 64.25, -1024.75,
                90.5f, -45.25f, 12.75f, 1_700_000_000_123L);
        Home otherHome = InMemoryHomeRepository.home("Base", "overworld", 0.0, 70.0, 0.0,
                0.0f, 180.0f, 0.0f, 1_600_000_000_000L);

        HomeStorage writer = new HomeStorage(file);
        writer.put(player, home);
        writer.put(other, otherHome);
        writer.save();

        HomeStorage reader = new HomeStorage(file);
        reader.load();

        Home loaded = reader.get(player, "Cabin");
        assertNotNull(loaded);
        assertEquals("Cabin", loaded.getName());
        assertEquals("world_nether", loaded.getWorld());
        assertEquals(12.5, loaded.getX(), 1e-9);
        assertEquals(64.25, loaded.getY(), 1e-9);
        assertEquals(-1024.75, loaded.getZ(), 1e-9);
        assertEquals(90.5f, loaded.getPitch(), 1e-6f);
        assertEquals(-45.25f, loaded.getYaw(), 1e-6f);
        assertEquals(12.75f, loaded.getRoll(), 1e-6f);
        assertEquals(1_700_000_000_123L, loaded.getCreatedAt());

        Home loadedOther = reader.get(other, "Base");
        assertNotNull(loadedOther);
        assertEquals("overworld", loadedOther.getWorld());
        assertEquals(180.0f, loadedOther.getYaw(), 1e-6f);

        assertEquals(1, reader.count(player));
        assertEquals(1, reader.count(other));
        assertNull(reader.get(other, "Cabin"));
    }

    @Test
    void saveCreatesMissingParentDirectories() throws Exception {
        Path file = tempDir.resolve("nested").resolve("deeper").resolve("homes.json");
        HomeStorage storage = new HomeStorage(file);
        storage.put(UUID.randomUUID(), InMemoryHomeRepository.home("home"));

        storage.save();

        assertTrue(Files.isDirectory(file.getParent()));
        assertTrue(Files.exists(file));
    }

    @Test
    void loadOnMalformedJsonThrowsIOException() throws Exception {
        Path file = tempDir.resolve("homes.json");
        Files.writeString(file, "{ this is not valid json");
        HomeStorage storage = new HomeStorage(file);

        assertThrows(IOException.class, storage::load);
    }

    @Test
    void saveLeavesNoTemporaryFileBehind() throws Exception {
        Path file = tempDir.resolve("homes.json");
        HomeStorage storage = new HomeStorage(file);
        storage.put(UUID.randomUUID(), InMemoryHomeRepository.home("home"));

        storage.save();

        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
        try (Stream<Path> entries = Files.list(file.getParent())) {
            List<Path> leftovers = entries
                    .filter(path -> path.getFileName().toString().endsWith(".tmp"))
                    .toList();
            assertTrue(leftovers.isEmpty(), "unexpected temporary files: " + leftovers);
        }
    }

    @Test
    void listReturnsHomesSortedByName() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.randomUUID();
        HomeStorage storage = new HomeStorage(file);
        storage.put(player, InMemoryHomeRepository.home("zulu"));
        storage.put(player, InMemoryHomeRepository.home("alpha"));
        storage.put(player, InMemoryHomeRepository.home("mike"));
        storage.save();

        assertEquals(List.of("alpha", "mike", "zulu"),
                storage.list(player).stream().map(Home::getName).toList());
    }

    @Test
    void removeOfTheLastHomeEmptiesTheStore() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.randomUUID();
        HomeStorage storage = new HomeStorage(file);
        storage.put(player, InMemoryHomeRepository.home("home"));
        storage.save();

        assertTrue(storage.remove(player, "home"));

        assertEquals(0, storage.count(player));
        assertNull(storage.get(player, "home"));
        assertTrue(storage.list(player).isEmpty());
    }

    @Test
    void failedLoadPreservesPreviouslyLoadedData() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.randomUUID();
        HomeStorage storage = new HomeStorage(file);
        storage.put(player, InMemoryHomeRepository.home("keep", "world", 4.0, 5.0, 6.0,
                7.0f, 8.0f, 9.0f, 42L));
        storage.save();
        storage.load();
        assertNotNull(storage.get(player, "keep"));

        Files.writeString(file, "definitely not json {");

        assertThrows(IOException.class, storage::load);

        Home kept = storage.get(player, "keep");
        assertNotNull(kept);
        assertEquals("keep", kept.getName());
        assertEquals("world", kept.getWorld());
        assertEquals(4.0, kept.getX(), 1e-9);
        assertEquals(42L, kept.getCreatedAt());
        assertEquals(1, storage.count(player));
    }

    @Test
    void secondSaveKeepsThePreviousStateInABackup() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.randomUUID();
        HomeStorage storage = new HomeStorage(file);
        storage.put(player, InMemoryHomeRepository.home("Cabin", "world", 1.0, 2.0, 3.0,
                0.0f, 0.0f, 0.0f, 10L));
        storage.save();
        storage.put(player, InMemoryHomeRepository.home("Base", "overworld", 4.0, 5.0, 6.0,
                0.0f, 0.0f, 0.0f, 20L));
        storage.save();

        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        assertTrue(Files.exists(backup), "expected a backup file at " + backup);

        HomeStorage restored = new HomeStorage(backup);
        restored.load();

        assertEquals(1, restored.count(player));
        assertNotNull(restored.get(player, "Cabin"));
        assertNull(restored.get(player, "Base"));
        assertEquals("world", restored.get(player, "Cabin").getWorld());
    }

    @Test
    void loadRejectsAnUnsupportedVersion() throws Exception {
        Path file = tempDir.resolve("homes.json");
        Files.writeString(file, """
                {
                  "version": 2,
                  "players": {}
                }
                """);
        HomeStorage storage = new HomeStorage(file);

        assertThrows(IOException.class, storage::load);
    }

    @Test
    void loadSkipsAnInvalidHomeEntryAndKeepsValidOnes() throws Exception {
        Path file = tempDir.resolve("homes.json");
        UUID player = UUID.fromString("11111111-2222-3333-4444-555555555555");
        Files.writeString(file, """
                {
                  "version": 1,
                  "players": {
                    "11111111-2222-3333-4444-555555555555": {
                      "Cabin": {
                        "name": "Cabin",
                        "world": "world",
                        "x": 1.5,
                        "y": 64.0,
                        "z": -2.5,
                        "pitch": 0.0,
                        "yaw": 90.0,
                        "roll": 0.0,
                        "createdAt": 1700000000000
                      },
                      "Broken": {
                        "name": "Broken",
                        "world": "   ",
                        "x": 1.5,
                        "y": 64.0,
                        "z": -2.5,
                        "pitch": 0.0,
                        "yaw": 90.0,
                        "roll": 0.0,
                        "createdAt": 1700000000000
                      }
                    }
                  }
                }
                """);
        HomeStorage storage = new HomeStorage(file);

        storage.load();

        assertEquals(1, storage.count(player));
        Home valid = storage.get(player, "Cabin");
        assertNotNull(valid);
        assertEquals("world", valid.getWorld());
        assertEquals(1.5, valid.getX(), 1e-9);
        assertNull(storage.get(player, "Broken"));
    }
}
