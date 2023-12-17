package pl.smartbin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeaconAgent extends Agent {

    private List<AID> binAgents = new ArrayList<>();

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


        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("beacon");
        sd.setName("BeaconAgent_"+ UUID.randomUUID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            fe.printStackTrace();
        }

        Behaviour bh1 = new TickerBehaviour(this, 2000) {
            public void onTick() {
                if (binAgents.isEmpty()) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription serviceDescription = new ServiceDescription();
                    serviceDescription.setType("bin");
                    template.addServices(serviceDescription);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for (DFAgentDescription dfAgentDescription : result) {
                            binAgents.add(dfAgentDescription.getName());
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        addBehaviour(bh1);

        Behaviour b = new CyclicBehaviour(this) {

            public void action() {

                ACLMessage msg = receive();
                if(msg != null) {
                    switch(msg.getPerformative()) {

                        case ACLMessage.INFORM:
                            System.out.println(getAID().getName() + ": " + msg.getContent() + " [from " + msg.getSender().getName() + "]");
                            break;

                        case ACLMessage.QUERY_IF:
                            // TODO
                            break;

                        case ACLMessage.PROPOSE:
                            System.out.println(getAID().getName() + ": " + msg.getContent() + " [from " + msg.getSender().getName() + "]");

                            for (AID binAgent : binAgents) {
                                ACLMessage message = new ACLMessage(ACLMessage.QUERY_REF);
                                message.setLanguage("English");
                                message.setOntology("test-ontology");
                                message.addReceiver(binAgent);
                                String cont = "What is your current used capacity?";
                                message.setContent(cont);
                                System.out.println(getAID().getName() + ": " + cont + " [to " + binAgent.getName() + "]");
                                send(message);
                            }
                            // TODO
                            String cont = "I don't know yet :(";

                            send(getResponse(msg, ACLMessage.CONFIRM, cont));
                            System.out.println(getAID().getName() + ": " + cont + " [to " + msg.getSender().getName() + "]");

                            break;

                        case ACLMessage.NOT_UNDERSTOOD:
                            // TODO
                            break;
                    }
                }
                else block();
            }
        };

        addBehaviour(b);
    }
}
