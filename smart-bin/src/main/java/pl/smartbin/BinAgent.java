package pl.smartbin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
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

import javax.swing.*;
import java.util.Random;

import static pl.smartbin.utils.LoggingUtils.logReceiveMsg;

public class BinAgent extends Agent {

    private BinData state;
    private String regionId;
    private AID beaconAID;
    private MainPlane gui;

    protected void setup() {
        gui = MainPlane.getInstance();

        state = new BinData(new Location(0, 0), new Random().nextInt(47, 49));

        System.out.println("Setting up '" + getAID().getName() + "'");
        this.regionId = (String) this.getArguments()[0];
        AgentUtils.registerAgent(this, AgentType.BIN, AgentUtils.getRegionProp(regionId));

        var discoveryBh = new TickerBehaviour(this, 1000) {
            @Override
            public void onTick() {
                var prop = new Property();
                prop.setName("region_id");
                prop.setValue(regionId);

                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType("beacon");
                serviceDescription.addProperties(prop);
                template.addServices(serviceDescription);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    //System.out.printf("[bin %s] Found %d beacons\n", getName(), result.length);
                    if (result.length == 1) {
                        beaconAID = result[0].getName();
                        //    System.out.printf("[bin %s] Found beacon %s\n", getName(), beaconAID.getName());
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        };

        var increaseUsedCapacityRandomlyBh = new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {
                int increaseBy = (new Random()).nextInt(0, 2);
                state.usedCapacityPct = Math.min(100, state.usedCapacityPct + increaseBy);
                SwingUtilities.invokeLater(() -> gui.updateBinFill(getLocalName(), beaconAID.getLocalName(), state.usedCapacityPct));

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
                logReceiveMsg(AgentType.BIN, myAgent.getName(), propose);
                int decision;
                if (state.usedCapacityPct >= 50) {
                    decision = ACLMessage.ACCEPT_PROPOSAL;
                } else {
                    decision = ACLMessage.REJECT_PROPOSAL;
                }
                return MessageUtils.createReply(propose, decision, null);
            }
        });

        addBehaviour(discoveryBh);
        addBehaviour(informCapacityBh);
        addBehaviour(messageRetrievalBh);
        addBehaviour(increaseUsedCapacityRandomlyBh);
    }


    private void handleInform(ACLMessage msg) {
        logReceiveMsg(AgentType.GARBAGE_COLLECTOR, getName(), msg);
        LoggingUtils.log(AgentType.BIN, getName(), "emptying capacity");
        state.usedCapacityPct = new Random().nextInt(45, 47);
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
