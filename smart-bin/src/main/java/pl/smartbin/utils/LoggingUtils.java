package pl.smartbin.utils;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import pl.smartbin.AgentType;

public class LoggingUtils {

    public static void log(AgentType agentType, String agentName, String info) {
        System.out.printf("[%s] %s%n", agentName, info);
    }

    public static void logSendMsg(AgentType agentType, String senderName, String receiverName) {
        logSendMsg(agentType, senderName, receiverName, null);
    }

    public static void logSendMsg(AgentType agentType, String senderName, String receiverName, String content) {
        log(agentType, senderName, "sent message to %s, content: %s".formatted(receiverName, content));
    }

    public static void logSendMsg(AgentType agentType, ACLMessage msg) {
        msg.getAllReceiver()
                .forEachRemaining(receiverAID -> logSendMsg(agentType, msg.getSender().getName(),
                        ((AID) receiverAID).getName(), msg.getContent()));
    }

    public static void logReceiveMsg(AgentType agentType, String receiverName, ACLMessage message) {
        log(agentType, receiverName, "received message of type [%s] from %s, content: %s".formatted(
                message.getPerformative(), message.getSender().getName(), message.getContent()));
    }

    public static void logFsmState(FSMBehaviour fsm, Behaviour b) {
        System.out.printf("[FSM %s @ %s] state: %s\n", fsm.getClass().getName(), fsm.getAgent().getLocalName(), b.getBehaviourName());
    }
}
