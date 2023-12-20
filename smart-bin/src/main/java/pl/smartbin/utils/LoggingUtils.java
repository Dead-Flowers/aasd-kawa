package pl.smartbin.utils;

import jade.lang.acl.ACLMessage;
import pl.smartbin.AgentType;

public class LoggingUtils {

    public static void log(AgentType agentType, String agentName, String info) {
        System.out.printf("[%s %s] %s%n", agentType.getCode(), agentName, info);
    }

    public static void logSendMsg(AgentType agentType, String senderName, String receiverName) {
        logSendMsg(agentType, senderName, receiverName,null);
    }

    public static void logSendMsg(AgentType agentType, String senderName, String receiverName, String content) {
        log(agentType, senderName, "sent message to %s, content: %s".formatted(receiverName, content));
    }

    public static void logReceiveMsg(AgentType agentType, String receiverName, ACLMessage message) {
        log(agentType, receiverName, "received message of type [%s] from %s, content: %s".formatted(
                message.getPerformative(), message.getSender().getName(), message.getContent()));
    }
}
