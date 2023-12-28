package pl.smartbin.agent.bin;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Random;

public class Bin implements Serializable {

    @Getter
    @Setter
    private float latitude;
    @Getter
    @Setter
    private float longitude;
    @Getter
    @Setter
    private Integer usedCapacityPercent;
    private final Random rand;

    public Bin() {
        rand = new Random();
        this.latitude = rand.nextFloat(0, 100);
        this.longitude = rand.nextFloat(0, 100);
        usedCapacityPercent = 0;
    }

    public Bin(Integer value) {
        rand = new Random();
        this.latitude = rand.nextFloat(0, 100);
        this.longitude = rand.nextFloat(0, 100);
        usedCapacityPercent = value;
    }

    public void fill() {
        this.usedCapacityPercent += rand.nextInt(100);
        if (this.usedCapacityPercent > 100) {
            this.usedCapacityPercent = 100;
        }
    }

    public void removeTrash() {
        this.usedCapacityPercent = 0;
    }
}
