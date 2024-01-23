package pl.smartbin.agent.supervisor;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import lombok.Getter;
import lombok.Setter;
import pl.smartbin.AgentType;
import pl.smartbin.dto.Location;
import pl.smartbin.utils.AgentUtils;

import java.util.concurrent.TimeUnit;

public class SupervisorAgent extends Agent {

    @Getter
    private String region;

    @Getter
    private AID beaconAID;

    @Setter
    private boolean inProgress = false;

    @Getter
    @Setter
    private Location location;

    @Override
    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");

        this.region = (String) getArguments()[0];
        this.location = (Location) getArguments()[1];
        AgentUtils.registerAgent(this, AgentType.SUPERVISOR, AgentUtils.getRegionProp(region));

        var discoveryBh = new TickerBehaviour(this, TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick() {
                if (beaconAID == null) {
                    beaconAID = AgentUtils.findBeacon(myAgent, AgentType.SUPERVISOR, region);
                }
            }
        };

        var supervisorBh = new SupervisorBehaviour(this);

        var schedulerFsm = new FSMBehaviour(this);

        var schedulerWaitBh = new WakerBehaviour(this, 5000) {

        };
        schedulerFsm.registerFirstState(schedulerWaitBh, "Wait");
        schedulerFsm.registerState(supervisorBh, "Supervisor");

        schedulerFsm.registerDefaultTransition("Wait", "Supervisor");
        schedulerFsm.registerDefaultTransition("Supervisor", "Wait", new String[]{"Wait", "Supervisor"});

        addBehaviour(discoveryBh);
        addBehaviour(schedulerFsm);
    }
}
