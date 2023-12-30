package pl.smartbin.dto;

import java.util.Objects;

public final class BinData {
    public final Location location;
    public Integer usedCapacityPct;

    public BinData(Location location, int usedCapacityPct) {
        this.location = location;
        this.usedCapacityPct = usedCapacityPct;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BinData) obj;
        return Objects.equals(this.location, that.location) &&
                Objects.equals(this.usedCapacityPct, that.usedCapacityPct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, usedCapacityPct);
    }

    @Override
    public String toString() {
        return "BinData[" +
                "location=" + location + ", " +
                "usedCapacityPct=" + usedCapacityPct + ']';
    }

}
