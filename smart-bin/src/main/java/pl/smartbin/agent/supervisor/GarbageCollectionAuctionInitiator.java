package pl.smartbin.agent.supervisor;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.util.leap.Iterator;
import pl.smartbin.AgentType;
import pl.smartbin.utils.MessageUtils;

import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.function.Supplier;

import static pl.smartbin.utils.LoggingUtils.log;
import static pl.smartbin.utils.LoggingUtils.logReceiveMsg;

public class GarbageCollectionAuctionInitiator extends ContractNetInitiator {

    private final Supplier<List<Integer>> binCapacitiesSupplier;

    public GarbageCollectionAuctionInitiator(Agent a, ACLMessage cfp, Supplier<List<Integer>> binCapacitiesSupplier) {
        super(a, cfp);
        this.binCapacitiesSupplier = binCapacitiesSupplier;
    }

    @Override
    protected void handlePropose(ACLMessage propose, Vector acceptances) {
        logReceiveMsg(AgentType.SUPERVISOR, myAgent.getName(), propose);
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        logReceiveMsg(AgentType.SUPERVISOR, myAgent.getName(), refuse);
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        logReceiveMsg(AgentType.SUPERVISOR, myAgent.getName(), inform);
        log(AgentType.SUPERVISOR, myAgent.getName(), "Finished garbage collection");
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        log(AgentType.SUPERVISOR, myAgent.getName(), "Handle all responses: " + responses.size());
        ACLMessage bestOffer = null;

        for (Object response : responses) {
            ACLMessage resp = (ACLMessage) response;
            ACLMessage reply = MessageUtils.createReply(resp, ACLMessage.REJECT_PROPOSAL, null);

            if (bestOffer == null) {
                bestOffer = reply;
            }

            acceptances.add(reply);
        }
        log(AgentType.SUPERVISOR, myAgent.getName(), "Acceptances : " + acceptances.size());

        if (bestOffer != null) {
            bestOffer.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        }
    }

    @Override
    protected void handleAllResultNotifications(Vector resultNotifications) {
        log(AgentType.SUPERVISOR, myAgent.getName(), "All result notifications");
    }
}
