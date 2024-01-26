package pl.smartbin;

import pl.smartbin.dto.BinData;

public interface IBinAgent {
    void overrideUsedCapacityPct(int newValue);

    BinData getData();

    String getCurrentBeacon();
}
