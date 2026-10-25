package assos.hytale.home.home;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class HomeService {

    private static final String DEFAULT_HOME_NAME = "home";
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");

    private final HomeRepository repository;
    private final String defaultHomeName;
    private final int maxHomes;
    private final int maxNameLength;
    private final boolean allowCrossWorld;

    public HomeService(HomeRepository repository, @Nullable String defaultHomeName, int maxHomes, int maxNameLength,
            boolean allowCrossWorld) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.maxHomes = Math.max(1, maxHomes);
        this.maxNameLength = Math.max(1, maxNameLength);
        this.allowCrossWorld = allowCrossWorld;
        this.defaultHomeName = sanitizeDefaultName(defaultHomeName);
    }

    public String normalizeName(@Nullable String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("A home name is required.");
        }
        String normalized = rawName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("A home name is required.");
        }
        if (normalized.length() > maxNameLength) {
            throw new IllegalArgumentException("Home names are limited to " + maxNameLength + " characters.");
        }
        if (NUMERIC_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Home names cannot be only numbers; use a name.");
        }
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Home names may only contain letters, digits, '_' and '-'.");
        }
        return normalized;
    }

    public SetHomeResult setHome(UUID playerId, String rawName, Home home) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(home, "home");
        String name = normalizeName(rawName);
        boolean exists = repository.get(playerId, name) != null;
        if (!exists && repository.count(playerId) >= maxHomes) {
            return new SetHomeResult(SetHomeStatus.LIMIT_REACHED, name);
        }
        home.setName(name);
        repository.put(playerId, home);
        repository.save();
        return new SetHomeResult(exists ? SetHomeStatus.UPDATED : SetHomeStatus.CREATED, name);
    }

    public Optional<Home> resolve(UUID playerId, @Nullable String requestedName) {
        if (requestedName != null) {
            String name = normalizeName(requestedName);
            return Optional.ofNullable(repository.get(playerId, name));
        }

        Home defaultHome = repository.get(playerId, defaultHomeName);
        if (defaultHome != null) {
            return Optional.of(defaultHome);
        }

        List<Home> homes = list(playerId);
        return homes.size() == 1 ? Optional.of(homes.get(0)) : Optional.empty();
    }

    public Optional<Home> resolveSelector(UUID playerId, @Nullable String selector) {
        if (selector == null) {
            return resolve(playerId, null);
        }

        String trimmed = selector.trim();
        if (NUMERIC_PATTERN.matcher(trimmed).matches()) {
            int index;
            try {
                index = Integer.parseInt(trimmed);
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            List<Home> homes = list(playerId);
            if (index < 1 || index > homes.size()) {
                return Optional.empty();
            }
            return Optional.of(homes.get(index - 1));
        }

        return resolve(playerId, selector);
    }

    public List<Home> list(UUID playerId) {
        return repository.list(playerId);
    }

    public Optional<String> delete(UUID playerId, String rawName) throws IOException {
        String name = normalizeName(rawName);
        if (!repository.remove(playerId, name)) {
            return Optional.empty();
        }
        repository.save();
        return Optional.of(name);
    }

    public String getDefaultHomeName() {
        return defaultHomeName;
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public boolean isCrossWorldAllowed() {
        return allowCrossWorld;
    }

    private String sanitizeDefaultName(@Nullable String rawName) {
        String candidate = rawName == null ? DEFAULT_HOME_NAME : rawName.trim().toLowerCase(Locale.ROOT);
        if (candidate.length() > maxNameLength) {
            candidate = candidate.substring(0, maxNameLength);
        }
        if (candidate.isEmpty() || NUMERIC_PATTERN.matcher(candidate).matches()
                || !NAME_PATTERN.matcher(candidate).matches()) {
            candidate = DEFAULT_HOME_NAME.substring(0, Math.min(DEFAULT_HOME_NAME.length(), maxNameLength));
        }
        return candidate;
    }

    public enum SetHomeStatus {
        CREATED,
        UPDATED,
        LIMIT_REACHED
    }

    public record SetHomeResult(SetHomeStatus status, String name) {
    }
}
