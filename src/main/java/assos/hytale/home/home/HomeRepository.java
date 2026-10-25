package assos.hytale.home.home;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface HomeRepository {

    Home get(UUID playerId, String name);

    List<Home> list(UUID playerId);

    int count(UUID playerId);

    void put(UUID playerId, Home home);

    boolean remove(UUID playerId, String name);

    void save() throws IOException;
}
