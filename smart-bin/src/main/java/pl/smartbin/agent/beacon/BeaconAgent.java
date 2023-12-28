package pl.smartbin.agent.beacon;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.MessageUtils;
import pl.smartbin.MessageProtocol;
import pl.smartbin.AgentType;

import static pl.smartbin.utils.LoggingUtils.logReceiveMsg;

public class BeaconAgent extends Agent {

    private final HashMap<AID, Integer> binCapacities = new HashMap<>();

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
        sd.setName(getAID().getName());
        var prop = new Property();
        prop.setName("region_id");
        prop.setValue(this.getArguments()[0]);
        sd.addProperties(prop);

        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            fe.printStackTrace();
        }

        Behaviour b = new CyclicBehaviour(this) {

            public void action() {

                ACLMessage msg = receive();
                if(msg != null) {

                    logReceiveMsg(AgentType.BEACON, getName(), msg);

                    switch(msg.getPerformative()) {

                        case ACLMessage.INFORM -> handleInform(msg);
                        case ACLMessage.QUERY_IF -> handleQueryIf(msg);

                    }
                }
                else block();
            }
        };

        addBehaviour(b);
    }

    private void handleInform(ACLMessage msg) {
        switch (msg.getProtocol()) {
            case MessageProtocol.Bin2Beacon_Capacity:
                var val = Integer.parseInt(msg.getContent());
                binCapacities.put(msg.getSender(), val);
                System.out.printf("[Beacon %s] Capacity of %s = %d\n", getName(), msg.getSender().getName(), val);
                break;
            default:
                break;
        }
    }

    private void handleQueryIf(ACLMessage msg) {
        switch (msg.getProtocol()) {
            case MessageProtocol.Supervisor2Beacon_Capacities:
                msg.createReply();
                send(MessageUtils.createReply(msg, ACLMessage.INFORM, JsonUtils.toJson(binCapacities.values())));
                break;
            default:
                break;
        }
    }
}
