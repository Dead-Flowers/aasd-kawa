package pl.smartbin;

import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.UUID;

public class BinAgent extends Agent {

    private Integer usedCapacityPercent = 52;

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
        sd.setType("bin");
        sd.setName("BinAgent_"+ UUID.randomUUID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            fe.printStackTrace();
        }

        Behaviour bh2 = new CyclicBehaviour(this) {

            public void action() {
                ACLMessage msg = receive();
                Location loc;
                if (msg != null) {

                    switch (msg.getPerformative()) {

                        case ACLMessage.ACCEPT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: accept proposal " + " [IN from + " + msg.getSender().getName() + "]");
                            break;

                        case ACLMessage.REJECT_PROPOSAL:
                            System.out.println(getAID().getName() + ": " + " received: reject proposal " + " [IN from + " + msg.getSender().getName() + "]");
                            break;

                        case ACLMessage.INFORM:
                            System.out.println(getAID().getName() + ": " + " received: inform " + " [IN from + " + msg.getSender().getName() + "]");
                            break;

                        case ACLMessage.CONFIRM:
                            System.out.println(getAID().getName() + ": " + " received: confirm " + " [IN from + " + msg.getSender().getName() + "]");
                            break;

                        case ACLMessage.QUERY_REF:
                            String cont = "My current used capacity: " + usedCapacityPercent;
                            send(getResponse(msg, ACLMessage.INFORM, cont));
                            MainApplication.updateBinState(getAID().getLocalName(), usedCapacityPercent);
                            System.out.println(getAID().getName() + ": " + cont + " [REPLY to " + msg.getSender().getName() + "]");
                            break;
                    }
                } else block();
            }

        };

        addBehaviour(bh2);
    }
}
