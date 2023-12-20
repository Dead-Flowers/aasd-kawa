package pl.smartbin;

import jade.core.AID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BinCapacities implements Serializable {

    private ArrayList<BinCapacity> binCapacities = new ArrayList<>();

    public void put(AID aid, Integer value) {
        binCapacities.stream()
                     .filter(bc -> bc.getAid().equals(aid))
                     .findAny()
                     .ifPresentOrElse(bc -> bc.setValue(value),
                                      () -> binCapacities.add(new BinCapacity(aid, value)));
    }


    @Getter
    @Setter
    @AllArgsConstructor
    private static class BinCapacity implements Serializable {
        private final AID aid;
        private Integer value;
    }
}
