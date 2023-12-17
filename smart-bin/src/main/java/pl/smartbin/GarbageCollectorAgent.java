package pl.smartbin;

import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GarbageCollectorAgent extends Agent {

    private List<AID> beaconAgents = new ArrayList<>();

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

        Behaviour bh2 = new CyclicBehaviour(this) {

            public void action() {
                ACLMessage rep = receive();
                Location loc;
                if (rep != null) {

                    switch (rep.getPerformative()) {

                        case ACLMessage.ACCEPT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: accept proposal " + " [IN from + " + rep.getSender().getName() + "]");
                            break;

                        case ACLMessage.REJECT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: reject proposal " + " [IN from + " + rep.getSender().getName() + "]");
                            break;

                        case ACLMessage.INFORM:
                            System.out.println(getAID().getName() + ": " + " received: inform " + " [IN from + " + rep.getSender().getName() + "]");
                            break;

                        case ACLMessage.CONFIRM:
                            System.out.println(getAID().getName() + ": " + " received: confirm " + " [IN from + " + rep.getSender().getName() + "]");
                            break;
                    }
                } else block();
            }

        };

        addBehaviour(bh2);
    }
}
