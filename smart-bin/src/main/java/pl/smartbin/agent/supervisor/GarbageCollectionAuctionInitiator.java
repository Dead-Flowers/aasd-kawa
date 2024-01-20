package pl.smartbin.agent.supervisor;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import pl.smartbin.AgentType;
import pl.smartbin.dto.BinData;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.function.Supplier;

import static pl.smartbin.utils.LoggingUtils.*;

public class GarbageCollectionAuctionInitiator extends ContractNetInitiator {

    private final SupervisorAgent agent;
    private final Supplier<ACLMessage> cfpSupplier;
    private final Supplier<Map<AID, BinData>> binCapacitiesSupplier;

    public GarbageCollectionAuctionInitiator(SupervisorAgent a, Supplier<ACLMessage> cfp, Supplier<Map<AID, BinData>> binCapacitiesSupplier) {
        super(a, null);
        this.cfpSupplier = cfp;
        this.agent = a;
        this.binCapacitiesSupplier = binCapacitiesSupplier;
    }

    @Override
    protected void handleStateEntered(Behaviour state) {
        super.handleStateEntered(state);
        logFsmState(this, state);
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
            log(AgentType.SUPERVISOR, myAgent.getName(), "Perforative " + ((ACLMessage) response).getPerformative());
            if (ACLMessage.PROPOSE == resp.getPerformative()) {
                ACLMessage reply = MessageUtils.createReply(resp, ACLMessage.REJECT_PROPOSAL, null);

                acceptances.add(reply);
            }
        }
        if (acceptances.isEmpty())
            return;
        // TODO: for now, randomly choose best offer
        bestOffer = (ACLMessage) acceptances.get(new Random().nextInt(0, acceptances.size()));
        bestOffer.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        bestOffer.setContent(agent.getRegion());

        log(AgentType.SUPERVISOR, myAgent.getName(), "Acceptances : " + acceptances.size());

        for (Object accept : acceptances) {
            ACLMessage resp = (ACLMessage) accept;
            log(AgentType.SUPERVISOR, myAgent.getName(), "Accept performative " + ((ACLMessage) accept).getPerformative());
        }
    }

    @Override
    protected void handleAllResultNotifications(Vector resultNotifications) {
        log(AgentType.SUPERVISOR, myAgent.getName(), "All result notifications");
    }

    @Override
    protected Vector prepareCfps(ACLMessage cfp) {
        cfp = cfpSupplier.get();
        cfp.setContent(JsonUtils.toJson(binCapacitiesSupplier.get()));
        Vector x = super.prepareCfps(cfp);
        for (Object msg : x) {
            ((ACLMessage) msg).getAllReceiver().forEachRemaining(aid -> log(AgentType.SUPERVISOR, myAgent.getName(), "Preparing CFP for " + aid));

        }
        return x;
    }
}
