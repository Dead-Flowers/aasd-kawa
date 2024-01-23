package pl.smartbin.agent.garbage_collector;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import pl.smartbin.dto.Location;

@Getter
@RequiredArgsConstructor
public class GarbageCollectorData {

    private static final int MAX_CAPACITY = 100;

    @Setter
    private Location location;
    private int usedCapacity = 0;

    public GarbageCollectorData(Location location) {
        this.location = location;
    }

    public void addCapacity(int cap) {
        usedCapacity = Math.min(usedCapacity + cap, MAX_CAPACITY);
    }

    public boolean hasSpace(int capacityToAdd) {
        return usedCapacity < MAX_CAPACITY;
    }

    public boolean isFull() {
        return usedCapacity == MAX_CAPACITY;
    }

    public void clear() {
        usedCapacity = 0;
    }
}
