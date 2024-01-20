package pl.smartbin;

import jade.core.AID;
import pl.smartbin.dto.Location;

public interface IBeaconAgent {
    Location getLocation();

    AID getBetterAID();
}
