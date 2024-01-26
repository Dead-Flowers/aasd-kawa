package pl.smartbin;

import pl.smartbin.agent.garbage_collector.GarbageCollectorData;
import pl.smartbin.dto.Location;

public interface IGarbageCollectorAgent {
    Location getCurrentLocation();

    GarbageCollectorData getData();
}
