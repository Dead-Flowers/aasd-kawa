package pl.smartbin;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class ObjectOnPlane {
    @Setter
    protected float latitude;
    @Setter
    protected float longitude;
    protected final String name;

    protected ObjectOnPlane(float latitude, float longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }
}
