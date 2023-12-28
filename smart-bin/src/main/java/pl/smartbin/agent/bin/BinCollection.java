package pl.smartbin.agent.bin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;

public final class BinCollection implements Serializable {

    private static volatile BinCollection instance;
    private final ArrayList<BinAgentPair> binAgentPairs;

    private BinCollection() {
        this.binAgentPairs = new ArrayList<>();
    }

    public static BinCollection getInstance() {

        if (instance != null) {
            return instance;
        }
        synchronized (BinCollection.class) {
            if (instance == null) {
                instance = new BinCollection();
            }

            return instance;
        }
    }

    public void put(String name, Integer value) {
        binAgentPairs.stream()
                     .filter(bc -> bc.getAgentName().equals(name))
                     .findAny()
                     .ifPresentOrElse(bc -> bc.getBin().setUsedCapacityPercent(value),
                                      () -> binAgentPairs.add(new BinAgentPair(new Bin(value), name)));
    }

    public Bin getBin(String agentName) {
        return binAgentPairs.stream()
                .filter(bap -> bap.getAgentName().equals(agentName))
                .map(BinAgentPair::getBin)
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class BinAgentPair implements Serializable {
        private Bin bin;
        private String agentName;
    }
}
