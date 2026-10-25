package assos.hytale.home.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HomeServiceTest {

    private static final String DEFAULT_HOME = "home";
    private static final int MAX_HOMES = 3;
    private static final int MAX_NAME_LENGTH = 16;

    private InMemoryHomeRepository repository;
    private HomeService service;
    private UUID player;

    @BeforeEach
    void setUp() {
        repository = new InMemoryHomeRepository();
        service = new HomeService(repository, DEFAULT_HOME, MAX_HOMES, MAX_NAME_LENGTH, true);
        player = UUID.randomUUID();
    }

    // ---------------------------------------------------------------- normalizeName

    @Test
    void normalizeNameTrimsAndLowercases() {
        assertEquals("home", service.normalizeName("  HoMe  "));
    }

    @Test
    void normalizeNameKeepsLettersDigitsUnderscoreAndHyphen() {
        assertEquals("a_1-b2", service.normalizeName("a_1-b2"));
        assertEquals("my_home-2", service.normalizeName("My_Home-2"));
    }

    @Test
    void normalizeNameAcceptsExactlyMaxNameLength() {
        String name = "a".repeat(MAX_NAME_LENGTH);
        assertEquals(name, service.normalizeName(name));
    }

    @Test
    void normalizeNameRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName(null));
    }

    @Test
    void normalizeNameRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName(""));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("   "));
    }

    @Test
    void normalizeNameRejectsNamesLongerThanMaxNameLength() {
        String tooLong = "a".repeat(MAX_NAME_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName(tooLong));
    }

    @Test
    void normalizeNameRejectsSpacesAndPunctuation() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("my home"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("home!"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("home/2"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("home.name"));
    }

    // ---------------------------------------------------------------- setHome

    @Test
    void setHomeCreatesAndStoresTheNormalizedName() throws Exception {
        Home home = InMemoryHomeRepository.home("MyHome", "world", 10.5, 64.0, -3.5,
                1.0f, 2.0f, 3.0f, 123L);

        HomeService.SetHomeResult result = service.setHome(player, "  MyHome  ", home);

        assertEquals(HomeService.SetHomeStatus.CREATED, result.status());
        assertEquals("myhome", result.name());
        assertEquals(1, repository.saveCallCount());

        Home stored = repository.get(player, "myhome");
        assertNotNull(stored);
        assertEquals("myhome", stored.getName());
        assertEquals("world", stored.getWorld());
        assertEquals(10.5, stored.getX(), 1e-9);
        assertEquals(64.0, stored.getY(), 1e-9);
        assertEquals(-3.5, stored.getZ(), 1e-9);
        assertEquals(1.0f, stored.getPitch(), 1e-6f);
        assertEquals(2.0f, stored.getYaw(), 1e-6f);
        assertEquals(3.0f, stored.getRoll(), 1e-6f);
        assertEquals(123L, stored.getCreatedAt());
    }

    @Test
    void setHomeUpdatesAnExistingHome() throws Exception {
        service.setHome(player, "base", InMemoryHomeRepository.home("base", "world", 1, 2, 3,
                0f, 0f, 0f, 1L));
        Home replacement = InMemoryHomeRepository.home("base", "nether", 40, 50, 60,
                4f, 5f, 6f, 2L);

        HomeService.SetHomeResult result = service.setHome(player, "BASE", replacement);

        assertEquals(HomeService.SetHomeStatus.UPDATED, result.status());
        assertEquals("base", result.name());
        assertEquals(1, repository.count(player));
        assertEquals(2, repository.saveCallCount());

        Home stored = repository.get(player, "base");
        assertNotNull(stored);
        assertEquals("nether", stored.getWorld());
        assertEquals(40.0, stored.getX(), 1e-9);
        assertEquals(60.0, stored.getZ(), 1e-9);
        assertEquals(2L, stored.getCreatedAt());
    }

    @Test
    void setHomeRejectsANewNameAtTheLimit() throws Exception {
        HomeService limited = new HomeService(repository, DEFAULT_HOME, 2, MAX_NAME_LENGTH, true);
        limited.setHome(player, "one", InMemoryHomeRepository.home("one"));
        limited.setHome(player, "two", InMemoryHomeRepository.home("two"));
        assertEquals(2, repository.saveCallCount());

        HomeService.SetHomeResult result = limited.setHome(player, "three",
                InMemoryHomeRepository.home("three"));

        assertEquals(HomeService.SetHomeStatus.LIMIT_REACHED, result.status());
        assertEquals(2, repository.count(player));
        assertNull(repository.get(player, "three"));
        assertEquals(2, repository.saveCallCount());
    }

    @Test
    void setHomeAllowsOverwritingAtTheLimit() throws Exception {
        HomeService limited = new HomeService(repository, DEFAULT_HOME, 1, MAX_NAME_LENGTH, true);
        limited.setHome(player, "only", InMemoryHomeRepository.home("only", "world", 1, 2, 3,
                0f, 0f, 0f, 1L));

        HomeService.SetHomeResult result = limited.setHome(player, "only",
                InMemoryHomeRepository.home("only", "nether", 9, 8, 7, 0f, 0f, 0f, 5L));

        assertEquals(HomeService.SetHomeStatus.UPDATED, result.status());
        assertEquals(1, repository.count(player));
        assertEquals(2, repository.saveCallCount());
        assertNotNull(repository.get(player, "only"));
        assertEquals("nether", repository.get(player, "only").getWorld());
    }

    @Test
    void setHomeRejectsAnInvalidName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setHome(player, "bad name", InMemoryHomeRepository.home("bad name")));

        assertEquals(0, repository.count(player));
        assertEquals(0, repository.saveCallCount());
    }

    @Test
    void setHomePropagatesSaveFailure() {
        repository.setFailOnSave(true);

        assertThrows(IOException.class, () -> service.setHome(player, "cabin",
                InMemoryHomeRepository.home("cabin")));

        assertEquals(1, repository.saveCallCount());
        assertNotNull(repository.get(player, "cabin"));
    }

    // ---------------------------------------------------------------- resolve

    @Test
    void resolveReturnsTheRequestedHome() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolve(player, "cabin");

        assertTrue(resolved.isPresent());
        assertEquals("cabin", resolved.get().getName());
    }

    @Test
    void resolveReturnsEmptyForAnAbsentExplicitName() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        assertTrue(service.resolve(player, "missing").isEmpty());
    }

    @Test
    void resolveNullReturnsTheDefaultHome() {
        repository.put(player, InMemoryHomeRepository.home(DEFAULT_HOME, "default-world", 1, 2, 3,
                0f, 0f, 0f, 1L));
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolve(player, null);

        assertTrue(resolved.isPresent());
        assertEquals(DEFAULT_HOME, resolved.get().getName());
        assertEquals("default-world", resolved.get().getWorld());
    }

    @Test
    void resolveNullFallsBackToTheOnlyHome() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolve(player, null);

        assertTrue(resolved.isPresent());
        assertEquals("cabin", resolved.get().getName());
    }

    @Test
    void resolveNullReturnsEmptyWithoutAnyHome() {
        assertTrue(service.resolve(player, null).isEmpty());
    }

    @Test
    void resolveNullReturnsEmptyWithoutDefaultWhenSeveralHomesExist() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));
        repository.put(player, InMemoryHomeRepository.home("mine"));

        assertTrue(service.resolve(player, null).isEmpty());
    }

    // ---------------------------------------------------------------- resolveSelector

    @Test
    void resolveSelectorNullReturnsTheDefaultHome() {
        repository.put(player, InMemoryHomeRepository.home(DEFAULT_HOME));
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolveSelector(player, null);

        assertTrue(resolved.isPresent());
        assertEquals(DEFAULT_HOME, resolved.get().getName());
    }

    @Test
    void resolveSelectorNullFallsBackToTheOnlyHome() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolveSelector(player, null);

        assertTrue(resolved.isPresent());
        assertEquals("cabin", resolved.get().getName());
    }

    @Test
    void resolveSelectorNullReturnsEmptyWithoutAnyHome() {
        assertTrue(service.resolveSelector(player, null).isEmpty());
    }

    @Test
    void resolveSelectorNullReturnsEmptyWithoutDefaultWhenSeveralHomesExist() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));
        repository.put(player, InMemoryHomeRepository.home("mine"));

        assertTrue(service.resolveSelector(player, null).isEmpty());
    }

    @Test
    void resolveSelectorResolvesOneBasedIndexInNameOrder() {
        repository.put(player, InMemoryHomeRepository.home("zulu"));
        repository.put(player, InMemoryHomeRepository.home("alpha"));
        repository.put(player, InMemoryHomeRepository.home("mike"));

        assertEquals("alpha", service.resolveSelector(player, "1").orElseThrow().getName());
        assertEquals("mike", service.resolveSelector(player, "2").orElseThrow().getName());
        assertEquals("zulu", service.resolveSelector(player, "3").orElseThrow().getName());
    }

    @Test
    void resolveSelectorRejectsZeroAndOutOfRangeIndexes() {
        repository.put(player, InMemoryHomeRepository.home("alpha"));
        repository.put(player, InMemoryHomeRepository.home("mike"));

        assertTrue(service.resolveSelector(player, "0").isEmpty());
        assertTrue(service.resolveSelector(player, "3").isEmpty());
    }

    @Test
    void resolveSelectorTreatsLeadingZerosAsANumber() {
        repository.put(player, InMemoryHomeRepository.home("alpha"));
        repository.put(player, InMemoryHomeRepository.home("mike"));
        repository.put(player, InMemoryHomeRepository.home("zulu"));

        assertTrue(service.resolveSelector(player, "007").isEmpty());
    }

    @Test
    void resolveSelectorLeadingZerosResolveTheSeventhHome() {
        for (String name : List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot",
                "golf", "hotel")) {
            repository.put(player, InMemoryHomeRepository.home(name));
        }

        assertEquals("golf", service.resolveSelector(player, "007").orElseThrow().getName());
    }

    @Test
    void resolveSelectorResolvesANonNumericName() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<Home> resolved = service.resolveSelector(player, "cabin");

        assertTrue(resolved.isPresent());
        assertEquals("cabin", resolved.get().getName());
        assertTrue(service.resolveSelector(player, "missing").isEmpty());
    }

    @Test
    void resolveSelectorRejectsAnInvalidNonNumericName() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        assertThrows(IllegalArgumentException.class,
                () -> service.resolveSelector(player, "bad name"));
    }

    // ---------------------------------------------------------------- numeric names

    @Test
    void normalizeNameRejectsPurelyNumericNames() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("123"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("007"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeName("0"));
    }

    @Test
    void setHomeRejectsPurelyNumericNames() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setHome(player, "123", InMemoryHomeRepository.home("123")));
        assertThrows(IllegalArgumentException.class,
                () -> service.setHome(player, " 007 ", InMemoryHomeRepository.home("007")));

        assertEquals(0, repository.count(player));
        assertEquals(0, repository.saveCallCount());
    }

    @Test
    void constructorSanitizesANumericDefaultName() {
        HomeService sanitized = new HomeService(repository, "123", MAX_HOMES, MAX_NAME_LENGTH, true);

        String defaultName = sanitized.getDefaultHomeName();
        assertFalse(defaultName.matches("\\d+"), "default name must not be purely numeric");
        assertEquals(defaultName, sanitized.normalizeName(defaultName));
    }

    // ---------------------------------------------------------------- list

    @Test
    void listReturnsOnlyTheHomesOfThatPlayer() {
        UUID other = UUID.randomUUID();
        repository.put(player, InMemoryHomeRepository.home("zeta"));
        repository.put(player, InMemoryHomeRepository.home("alpha"));
        repository.put(other, InMemoryHomeRepository.home("other-home"));

        List<Home> homes = service.list(player);

        assertEquals(List.of("alpha", "zeta"),
                homes.stream().map(Home::getName).toList());
        assertEquals(1, service.list(other).size());
        assertTrue(service.list(UUID.randomUUID()).isEmpty());
    }

    // ---------------------------------------------------------------- delete

    @Test
    void deleteNormalizesRemovesAndSaves() throws Exception {
        repository.put(player, InMemoryHomeRepository.home("myhome"));
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<String> deleted = service.delete(player, "  MyHome  ");

        assertEquals(Optional.of("myhome"), deleted);
        assertNull(repository.get(player, "myhome"));
        assertNotNull(repository.get(player, "cabin"));
        assertEquals(1, repository.saveCallCount());
    }

    @Test
    void deleteMissingHomeReturnsEmptyAndDoesNotSave() throws Exception {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        Optional<String> deleted = service.delete(player, "ghost");

        assertTrue(deleted.isEmpty());
        assertEquals(1, repository.count(player));
        assertEquals(0, repository.saveCallCount());
    }

    @Test
    void deleteInvalidNameThrows() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));

        assertThrows(IllegalArgumentException.class, () -> service.delete(player, "bad name"));
        assertEquals(1, repository.count(player));
        assertEquals(0, repository.saveCallCount());
    }

    @Test
    void deletePropagatesSaveFailure() {
        repository.put(player, InMemoryHomeRepository.home("cabin"));
        repository.setFailOnSave(true);

        assertThrows(IOException.class, () -> service.delete(player, "cabin"));

        assertEquals(1, repository.saveCallCount());
        assertNull(repository.get(player, "cabin"));
    }

    // ---------------------------------------------------------------- constructor

    @Test
    void constructorSanitizesANullDefaultName() {
        HomeService sanitized = new HomeService(repository, null, MAX_HOMES, MAX_NAME_LENGTH, true);

        assertNotNull(sanitized.getDefaultHomeName());
        assertFalse(sanitized.getDefaultHomeName().isBlank());
        assertEquals(sanitized.getDefaultHomeName(),
                sanitized.normalizeName(sanitized.getDefaultHomeName()));
    }

    @Test
    void constructorSanitizesABlankDefaultName() {
        HomeService sanitized = new HomeService(repository, "   ", MAX_HOMES, MAX_NAME_LENGTH, true);

        assertFalse(sanitized.getDefaultHomeName().isBlank());
        assertEquals(sanitized.getDefaultHomeName(),
                sanitized.normalizeName(sanitized.getDefaultHomeName()));
    }

    @Test
    void constructorSanitizesAnInvalidDefaultName() {
        HomeService sanitized = new HomeService(repository, "my home!", MAX_HOMES, MAX_NAME_LENGTH,
                true);

        assertFalse(sanitized.getDefaultHomeName().isBlank());
        assertEquals(sanitized.getDefaultHomeName(),
                sanitized.normalizeName(sanitized.getDefaultHomeName()));
    }

    @Test
    void constructorClampsMaxHomesToAtLeastOne() {
        assertEquals(1, new HomeService(repository, DEFAULT_HOME, 0, MAX_NAME_LENGTH, true)
                .getMaxHomes());
        assertEquals(1, new HomeService(repository, DEFAULT_HOME, -5, MAX_NAME_LENGTH, true)
                .getMaxHomes());
    }

    @Test
    void constructorClampsMaxNameLengthToAtLeastOne() {
        HomeService clamped = new HomeService(repository, DEFAULT_HOME, MAX_HOMES, 0, true);

        assertEquals("a", clamped.normalizeName("a"));
        assertThrows(IllegalArgumentException.class, () -> clamped.normalizeName("ab"));
    }

    @Test
    void exposesConfiguration() {
        HomeService strict = new HomeService(repository, DEFAULT_HOME, 7, 12, false);

        assertEquals(DEFAULT_HOME, strict.getDefaultHomeName());
        assertEquals(7, strict.getMaxHomes());
        assertFalse(strict.isCrossWorldAllowed());
        assertTrue(service.isCrossWorldAllowed());
    }
}
