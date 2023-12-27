package pl.smartbin.agent.garbage_collector;

import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetResponder;
import pl.smartbin.AgentType;
import pl.smartbin.MessageProtocol;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static pl.smartbin.utils.MessageUtils.createReply;

public class GarbageCollectorAgent extends Agent {

    private List<AID> beaconAgents = new ArrayList<>();

    private GarbageCollector currentLocation = new GarbageCollector(new Random().nextFloat(0, 100), new Random().nextFloat(0, 100));

    protected Location parseAMSResponse(ACLMessage response) {
        Result results = null;
        try {
            results = (Result) getContentManager().extractContent(response);
        } catch (Exception e) {
        }

        Iterator it = results.getItems().iterator();

        Location loc = null;
        if (it.hasNext()) {
            loc = (Location) it.next();
        }

        return loc;
    }

    private ACLMessage prepareRequestToAMS(AID agent) {

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

        request.addReceiver(getAMS());
        request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
        request.setOntology(MobilityOntology.NAME);
        request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        Action act = new Action();
        act.setActor(getAMS());

        WhereIsAgentAction action = new WhereIsAgentAction();
        action.setAgentIdentifier(agent);
        act.setAction(action);

        try {
            getContentManager().fillContent(request, act);
        } catch (Exception e) {
        }

        return request;
    }

    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");

        AgentUtils.registerAgent(this, AgentType.GARBAGE_COLLECTOR);

        Behaviour bh1 = new TickerBehaviour(this, 2000) {
            public void onTick() {
                if (beaconAgents.isEmpty()) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription serviceDescription = new ServiceDescription();
                    serviceDescription.setType("beacon");
                    template.addServices(serviceDescription);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for (DFAgentDescription dfAgentDescription : result) {
                            beaconAgents.add(dfAgentDescription.getName());
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (AID beaconAgent : beaconAgents) {
                        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
                        msg.setLanguage("English");
                        msg.setOntology("test-ontology");
                        msg.addReceiver(beaconAgent);
                        String cont = "Do you have trash?";
                        msg.setContent(cont);
                        System.out.println(getAID().getName() + ": " + cont + " [to " + beaconAgent.getName() + "]");
                        send(msg);
                    }
                }
            }
        };

        addBehaviour(bh1);

//        Behaviour bh2 = new CyclicBehaviour(this) {
//
//            public void action() {
//
//                ACLMessage rep = receive();
//                Location loc;
//                // TODO: blocking receive
//                if (rep != null) {
//
//                    switch (rep.getPerformative()) {
//
//                        case ACLMessage.CFP:
//                            System.out.println(getAID().getName() + ": " + " received: call for proposal " + " [IN from + " + rep.getSender().getName() + "]");
//                            handleCfp(rep);
//                            break;
//
//                        case ACLMessage.ACCEPT_PROPOSAL:
//                            System.out.println(getAID().getName() + ": " + " received: accept proposal " + " [IN from + " + rep.getSender().getName() + "]");
//                            handleAcceptProposal(rep);
//                            break;
//
//                        case ACLMessage.REJECT_PROPOSAL:
//                            System.out.println(getAID().getName() + ": " + " received: reject proposal " + " [IN from + " + rep.getSender().getName() + "]");
//                            break;
//
//                        case ACLMessage.INFORM:
//                            System.out.println(getAID().getName() + ": " + " received: inform " + " [IN from + " + rep.getSender().getName() + "]");
//                            break;
//
//                        case ACLMessage.CONFIRM:
//                            System.out.println(getAID().getName() + ": " + " received: confirm " + " [IN from + " + rep.getSender().getName() + "]");
//                            currentLocation.setLatitude(new Random().nextFloat(0, 100));
//                            currentLocation.setLongitude(new Random().nextFloat(0, 100));
//                            MainApplication.updateGarbageCollectorLocation(getAID().getLocalName(), currentLocation);
//                            break;
//                    }
//                } else block();
//            }
//
//        };
//        bh2.reset();

        Behaviour bh2 = new GarbageCollectorBehaviour(this, () -> currentLocation);

        addBehaviour(bh2);
    }

    private void handleCfp(ACLMessage msg) {
        if (MessageProtocol.Supervisor2GarbageCollector_Offer.equals(msg.getProtocol())) {
            ACLMessage reply = createReply(msg, ACLMessage.PROPOSE, JsonUtils.toJson(currentLocation));
            send(reply);
            LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
        }
    }

    private void handleAcceptProposal(ACLMessage msg) {
        if (MessageProtocol.Supervisor2GarbageCollector_Offer.equals(msg.getProtocol())) {
            // TODO wywóz śmieci

            ACLMessage reply = createReply(msg, ACLMessage.INFORM, MessageProtocol.Supervisor2GarbageCollector_Finish, null);
            send(reply);
            LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
        }
    }
}
