package pl.smartbin.agent.garbage_collector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import pl.smartbin.AgentType;
import pl.smartbin.MessageProtocol;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static pl.smartbin.utils.MessageUtils.createMessage;
import static pl.smartbin.utils.MessageUtils.createReply;

public class GarbageCollectorAgent extends Agent {

    private List<AID> beaconAgents = new ArrayList<>();

    private GarbageCollector currentLocation = new GarbageCollector(new Random().nextFloat(0, 100), new Random().nextFloat(0, 100));


    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");

        AgentUtils.registerAgent(this, AgentType.GARBAGE_COLLECTOR);

//        Behaviour bh1 = new TickerBehaviour(this, 2000) {
//            public void onTick() {
//                if (beaconAgents.isEmpty()) {
//                    DFAgentDescription template = new DFAgentDescription();
//                    ServiceDescription serviceDescription = new ServiceDescription();
//                    serviceDescription.setType("beacon");
//                    template.addServices(serviceDescription);
//                    try {
//                        DFAgentDescription[] result = DFService.search(myAgent, template);
//                        for (DFAgentDescription dfAgentDescription : result) {
//                            beaconAgents.add(dfAgentDescription.getName());
//                        }
//                    } catch (FIPAException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    for (AID beaconAgent : beaconAgents) {
//                        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
//                        msg.setLanguage("English");
//                        msg.setOntology("test-ontology");
//                        msg.addReceiver(beaconAgent);
//                        String cont = "Do you have trash?";
//                        msg.setContent(cont);
//                        System.out.println(getAID().getName() + ": " + cont + " [to " + beaconAgent.getName() + "]");
//                        send(msg);
//                    }
//                }
//            }
//        };
//
//        addBehaviour(bh1);

        Behaviour bh2 = new CyclicBehaviour(this) {

            public void action() {

                ACLMessage rep = receive();
                Location loc;
                // TODO: blocking receive
                if (rep != null) {

                    switch (rep.getPerformative()) {

                        case ACLMessage.CFP:
                            System.out.println(getAID().getName() + ": " + " received: call for proposal " + " [IN from + " + rep.getSender().getName() + "]");
                            handleCfp(rep);
                            break;

                        case ACLMessage.ACCEPT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: accept proposal " + " [IN from + " + rep.getSender().getName() + "]");
                            handleAcceptProposal(rep);
                            break;

                        case ACLMessage.REJECT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: reject proposal " + " [IN from + " + rep.getSender().getName() + "]");
                            break;

                        case ACLMessage.INFORM:
                            System.out.println(getAID().getName() + ": " + " received: inform " + " [IN from + " + rep.getSender().getName() + "]");
                            break;

                        case ACLMessage.CONFIRM:
                            System.out.println(getAID().getName() + ": " + " received: confirm " + " [IN from + " + rep.getSender().getName() + "]");
                            currentLocation.setLatitude(new Random().nextFloat(0, 100));
                            currentLocation.setLongitude(new Random().nextFloat(0, 100));
//                            MainApplication.updateGarbageCollectorLocation(getAID().getLocalName(), currentLocation);
                            break;
                    }
                } else block();
            }

        };
        bh2.reset();

//        Behaviour bh2 = new GarbageCollectorBehaviour(this, () -> currentLocation);
//
        addBehaviour(bh2);
    }

    private void handleCfp(ACLMessage msg) {
        ACLMessage reply = createReply(msg, ACLMessage.PROPOSE, JsonUtils.toJson(currentLocation));
        send(reply);
        LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
    }

    private void handleAcceptProposal(ACLMessage msg) {
        if (MessageProtocol.Bin2GarbageCollector.equals(msg.getProtocol())) {
            handleSingleBinGarbageCollection(msg);
        } else {
            handleGarbageCollectionProcess(msg);
        }
    }

    private void handleSingleBinGarbageCollection(ACLMessage msg) {
        ACLMessage reply = createReply(msg, ACLMessage.INFORM, null);
        send(reply);
        LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
    }

    private void handleGarbageCollectionProcess(ACLMessage msg) {
        String regionId = msg.getContent();
        LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, getName(), "Starting garbage collection in region: " + regionId);
        sendGarbageCollectionPropose(regionId);

        ACLMessage reply = createReply(msg, ACLMessage.INFORM,  null);
        send(reply);
        LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
    }

    private void sendGarbageCollectionPropose(String regionId) {
        AID[] bins = AgentUtils.findBins(this, AgentType.GARBAGE_COLLECTOR, regionId);
        ACLMessage propose = createMessage(ACLMessage.PROPOSE, MessageProtocol.Bin2Beacon_Capacity, bins);

        send(propose);
        LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, propose);
    }
}
