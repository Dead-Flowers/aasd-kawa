package pl.smartbin.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import pl.smartbin.dto.Location;

import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationUtils {

    public static Location getRandomLocation() {
        Random rnd = new Random();
        return new Location(rnd.nextFloat(0, 100), rnd.nextFloat(0, 100));
    }

    public static double calculateDistance(Location from, Location to) {
        float x1 = from.longitude();
        float y1 = from.latitude();
        float x2 = to.longitude();
        float y2 = to.latitude();
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static Location calculateTargetLocation(Location from, Location to, double maxDist) {
        double dist = calculateDistance(from, to);
        if (dist <= maxDist) {
            return to;
        }

        float x1 = from.longitude();
        float y1 = from.latitude();
        float x2 = to.longitude();
        float y2 = to.latitude();

        double x_diff = x2 - x1;
        double y_diff = y2 - y1;

        double x_delta = x_diff * maxDist / dist;
        double y_delta = y_diff * maxDist / dist;

        return new Location((float) (from.longitude() + x_delta), (float) (from.latitude() + y_delta));
    }
}
