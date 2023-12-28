package pl.smartbin.agent.supervisor;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import lombok.Getter;
import lombok.Setter;
import pl.smartbin.AgentType;
import pl.smartbin.utils.AgentUtils;

import java.util.concurrent.TimeUnit;

public class SupervisorAgent extends Agent {

    @Getter
    private String region;

    @Getter
    private AID beaconAID;

    @Setter
    private boolean inProgress = false;

    @Override
    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");

        this.region = (String) getArguments()[0];
        AgentUtils.registerAgent(this, AgentType.SUPERVISOR, AgentUtils.getRegionProp(region));

        var discoveryBh = new TickerBehaviour(this, TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick() {
                if (beaconAID == null) {
                    beaconAID = AgentUtils.findBeacon(myAgent, AgentType.SUPERVISOR, region);
                }
            }
        };

        var collectionStartBh = new TickerBehaviour(this, TimeUnit.SECONDS.toMillis(2)) {

            @Override
            protected void onTick() {
                System.out.println("In progress: " + inProgress);
                if (beaconAID == null || inProgress) {
                    return;
                }
                inProgress = true;
                addBehaviour(new SupervisorBehaviour((SupervisorAgent) myAgent)
                                     .onBeforeEnd(() -> setInProgress(false)));
            }
        };

        addBehaviour(discoveryBh);
        addBehaviour(collectionStartBh);
    }
}
