package pl.smartbin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ProposeResponder;
import pl.smartbin.dto.BinData;
import pl.smartbin.dto.Location;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.Random;

public class BinAgent extends Agent implements IBinAgent {

    private BinData state;
    private Location location;
    private AID beaconAID;
    private MainPlane gui;

    public BinAgent() {
        super();
        this.registerO2AInterface(IBinAgent.class, this);
    }

    protected void setup() {
        gui = MainPlane.getInstance();

        System.out.println("Setting up '" + getAID().getName() + "'");
        this.beaconAID = (AID) this.getArguments()[0];
        this.location = (Location) this.getArguments()[1];
        state = new BinData(location, new Random().nextInt(0, 40));
        AgentUtils.registerAgent(this, AgentType.BIN); // AgentUtils.getRegionProp(regionId)

        var increaseUsedCapacityRandomlyBh = new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {
                int increaseBy = (new Random()).nextInt(0, 2);
                state.usedCapacityPct = Math.min(100, state.usedCapacityPct + increaseBy);

            }
        };

        var informCapacityBh = new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                sendUpdateStatusForCapacity();
            }
        };

        Behaviour messageRetrievalBh = new CyclicBehaviour(this) {

            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchProtocol(MessageProtocol.Bin2GarbageCollector));
                if (msg == null) {
                    block();
                    return;
                }
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    handleInform(msg);
                }
            }
        };

        addBehaviour(new ProposeResponder(this, MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE)) {
            @Override
            protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
                int decision;
                String content = null;
                if (state.usedCapacityPct >= 50) {
                    decision = ACLMessage.ACCEPT_PROPOSAL;
                    content = JsonUtils.toJson(state);
                } else {
                    LoggingUtils.log(AgentType.BIN, getLocalName(),
                            "My capacity used is below 50 percent (%s). Rejecting garbage collection proposal.".formatted(state.usedCapacityPct));
                    decision = ACLMessage.REJECT_PROPOSAL;
                }
                return MessageUtils.createReply(propose, decision, content);
            }
        });

//        addBehaviour(discoveryBh);
        addBehaviour(informCapacityBh);
        addBehaviour(messageRetrievalBh);
        addBehaviour(increaseUsedCapacityRandomlyBh);
    }

    public void overrideUsedCapacityPct(int newValue) {
        this.state.usedCapacityPct = newValue;
        sendUpdateStatusForCapacity();
    }

    @Override
    public BinData getData() {
        return state;
    }

    @Override
    public String getCurrentBeacon() {
        if (this.beaconAID != null) {
            return this.beaconAID.getLocalName();
        }
        return null;
    }

    private void handleInform(ACLMessage msg) {
        LoggingUtils.log(AgentType.BIN, getName(), "%s removed all the trash".formatted(msg.getSender().getLocalName()));
        state.usedCapacityPct = 0;
        sendUpdateStatusForCapacity();
    }

    private void sendUpdateStatusForCapacity() {
        if (beaconAID == null) {
            return;
        }
        var tempAID = new AID(beaconAID.getName(), true);
        send(MessageUtils.createMessage(ACLMessage.INFORM, MessageProtocol.Bin2Beacon_Capacity, JsonUtils.toJson(state), tempAID));
    }
}
