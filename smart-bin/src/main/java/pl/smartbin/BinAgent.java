package pl.smartbin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.Random;

import static pl.smartbin.utils.LoggingUtils.logReceiveMsg;

public class BinAgent extends Agent {

    private Integer usedCapacityPercent = 0;

    private String regionId;
    private AID beaconAID;

    protected ACLMessage getResponse(ACLMessage msg, int perf, String cont) {
        ACLMessage response = msg.createReply();
        response.addReplyTo(msg.getSender());
        response.setLanguage("English");
        response.setOntology("test-ontology");
        response.setPerformative(perf);
        response.setContent(cont);
        System.out.println(getAID().getName() + ": " + cont);
        return response;
    }

    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");
        this.regionId = (String) this.getArguments()[0];
        this.usedCapacityPercent = (new Random()).nextInt(0, 100);

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
                    System.out.printf("[bin %s] Found %d beacons\n", getName(), result.length);
                    if (result.length == 1) {
                        beaconAID = result[0].getName();
                        System.out.printf("[bin %s] Found beacon %s\n", getName(), beaconAID.getName());
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
                usedCapacityPercent = Math.min(100, usedCapacityPercent + increaseBy);
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
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }
                switch (msg.getPerformative()) {
                    case ACLMessage.INFORM -> handleInform(msg);
                    case ACLMessage.PROPOSE -> handlePropose(msg);
                }
            }

        };

        addBehaviour(discoveryBh);
        addBehaviour(informCapacityBh);
        addBehaviour(messageRetrievalBh);
        addBehaviour(increaseUsedCapacityRandomlyBh);
    }

    private void handlePropose(ACLMessage msg) {
        logReceiveMsg(AgentType.GARBAGE_COLLECTOR, getName(), msg);

        int decision;
        if (usedCapacityPercent >= 50) {
            decision = ACLMessage.ACCEPT_PROPOSAL;
        } else {
            decision = ACLMessage.REJECT_PROPOSAL;
        }
        LoggingUtils.log(AgentType.BIN, getName(), "decision " + decision);

        msg.createReply();
        send(MessageUtils.createReply(msg, decision, MessageProtocol.Bin2GarbageCollector ,null));
    }

    private void handleInform(ACLMessage msg) {
        logReceiveMsg(AgentType.GARBAGE_COLLECTOR, getName(), msg);
        LoggingUtils.log(AgentType.BIN, getName(), "emptying capacity");
        usedCapacityPercent = 0;
        sendUpdateStatusForCapacity();
    }

    private void sendUpdateStatusForCapacity() {
        if (beaconAID == null) {
            return;
        }
        send(MessageUtils.createMessage(ACLMessage.INFORM, MessageProtocol.Bin2Beacon_Capacity, usedCapacityPercent.toString(), beaconAID));
    }
}
