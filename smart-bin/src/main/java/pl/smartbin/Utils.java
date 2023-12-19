package pl.smartbin;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class Utils {
    public static ACLMessage createMessage(int type, String protocol, String message, AID... targets) {
        var msg = new ACLMessage(type);
        msg.setLanguage("English");
        msg.setProtocol(protocol);
        msg.setOntology("test-ontology");
        for(var target : targets)
            msg.addReceiver(target);
        msg.setContent(message);
        return msg;
    }
}
