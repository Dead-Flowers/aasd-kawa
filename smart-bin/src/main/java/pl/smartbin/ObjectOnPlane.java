package pl.smartbin;

import lombok.Getter;
import lombok.Setter;
import pl.smartbin.dto.Location;

@Getter
public abstract class ObjectOnPlane {
    @Setter
    protected Location location;
    protected final String name;

    protected ObjectOnPlane(Location location, String name) {
        this.location = location;
        this.name = name;
    }
}
