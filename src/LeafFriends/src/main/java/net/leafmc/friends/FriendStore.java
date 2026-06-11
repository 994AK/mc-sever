package net.leafmc.friends;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface FriendStore {
    List<FriendProfile> load() throws IOException;

    void save(Collection<FriendProfile> profiles) throws IOException;
}
