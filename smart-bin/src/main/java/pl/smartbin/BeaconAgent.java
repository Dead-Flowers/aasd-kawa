package pl.smartbin;

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
import pl.smartbin.dto.BinData;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.MessageUtils;

import javax.swing.*;
import java.util.HashMap;

import static pl.smartbin.utils.LoggingUtils.logReceiveMsg;

public class BeaconAgent extends Agent {
    private final HashMap<AID, BinData> binCapacities = new HashMap<>();
    private MainPlane gui;


    private void handleInform(ACLMessage msg) {
        switch (msg.getProtocol()) {
            case MessageProtocol.Bin2Beacon_Capacity:
                var val = JsonUtils.fromJson(msg.getContent(), BinData.class);
                binCapacities.put(msg.getSender(), val);
                System.out.printf("[Beacon %s] Capacity of %s = %d\n", getName(), msg.getSender().getName(), val.usedCapacityPct);
                break;
            default:
                break;
        }
    }

    private void handleQueryIf(ACLMessage msg) {
        switch (msg.getProtocol()) {
            case MessageProtocol.Supervisor2Beacon_Capacities:
                send(MessageUtils.createReply(msg, ACLMessage.INFORM, JsonUtils.toJson(binCapacities)));
                break;
            default:
                break;
        }
    }

    protected void setup() {
        gui = MainPlane.getInstance();
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
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        Behaviour b = new CyclicBehaviour(this) {

            public void action() {

                SwingUtilities.invokeLater(() -> gui.updateBeaconOnline(getLocalName()));
                ACLMessage msg = receive();
                if (msg != null) {
                    logReceiveMsg(AgentType.BEACON, getName(), msg);

                    switch (msg.getPerformative()) {

                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;

                        case ACLMessage.QUERY_IF:
                            handleQueryIf(msg);
                            break;

                        case ACLMessage.PROPOSE:
                            System.out.println(getAID().getName() + ": " + msg.getContent() + " [from " + msg.getSender().getName() + "]");
                            break;
                        case ACLMessage.NOT_UNDERSTOOD:
                            // TODO
                            break;
                    }
                } else block();
            }
        };

        addBehaviour(b);
    }
}
