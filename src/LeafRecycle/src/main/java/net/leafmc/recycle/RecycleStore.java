package net.leafmc.recycle;

import java.io.IOException;
import java.util.List;

public interface RecycleStore {
    List<RecycleEntry> load() throws IOException;

    void save(List<RecycleEntry> entries) throws IOException;
}
