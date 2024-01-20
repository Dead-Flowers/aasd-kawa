package pl.smartbin.agent.garbage_collector;

import java.awt.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class GarbageCollector implements Serializable {
    private float latitude;
    private float longitude;
    private transient Color color;

    public GarbageCollector(float latitude, float longitude, Color color) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.color = color;
    }

    public GarbageCollector(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        Random rand = new Random();
        this.color = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GarbageCollector that = (GarbageCollector) o;
        return Float.compare(latitude, that.latitude) == 0 && Float.compare(longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
