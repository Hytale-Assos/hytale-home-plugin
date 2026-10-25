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
}
