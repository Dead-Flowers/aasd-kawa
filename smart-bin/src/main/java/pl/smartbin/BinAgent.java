package pl.smartbin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.UUID;

import static pl.smartbin.Utils.*;

public class BinAgent extends Agent {

    private Integer usedCapacityPercent = 52;

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

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bin");
        sd.setName("BinAgent_" + UUID.randomUUID());

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }


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

        var informCapacityBh = new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (beaconAID == null) {
                    return;
                }
                send(Utils.createMessage(ACLMessage.INFORM, MessageProtocol.Bin2Beacon_Capacity, usedCapacityPercent.toString(), beaconAID));
            }
        };

        Behaviour bh2 = new CyclicBehaviour(this) {

            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }
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
            }

        };

        addBehaviour(discoveryBh);
        addBehaviour(informCapacityBh);
        addBehaviour(bh2);
    }
}
