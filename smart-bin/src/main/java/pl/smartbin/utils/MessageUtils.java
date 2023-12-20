package pl.smartbin.utils;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class MessageUtils {

    public static ACLMessage createMessage(int type, String protocol, AID... targets) {
        return createMessage(type, protocol, null, targets);
    }
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

    public static ACLMessage createReply(ACLMessage msg, int type, String content) {
        ACLMessage reply = msg.createReply(type);
        reply.setContent(content);

        return reply;
    }
}
