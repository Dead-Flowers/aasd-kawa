package pl.smartbin.agent.garbage_collector;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetResponder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import pl.smartbin.AgentType;
import pl.smartbin.GarbageCollector;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

import static pl.smartbin.utils.MessageUtils.createReply;

public class GarbageCollectorCfpBehaviour extends ContractNetResponder {

    @Data
    @AllArgsConstructor
    public static class Result {
        ACLMessage cfp;
        ACLMessage proposal;
        ACLMessage accepted;
        ACLMessage rejected;
    }

    public static final String COLLECT_GARBAGE = "Collect-Garbage";

    @Getter
    private Result result;

    private final Supplier<GarbageCollector> locationSupplier;

    public GarbageCollectorCfpBehaviour(Agent a, Supplier<GarbageCollector> locationSupplier) {
        super(a, createMessageTemplate(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET));
        this.locationSupplier = locationSupplier;
        registerLastState(new OneShotBehaviour(a) {
            @Override
            public void action() {
                var wasAccepted = result.accepted != null;
                LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "CFP done, accepted: " + wasAccepted);
            }
        }, "Final");
        registerDefaultTransition(DUMMY_FINAL, "Final", new String[]{"Final"});
    }

    @Override
    public int onEnd() {
        return result != null && result.accepted != null ? 1 : 0;
    }

    @Override
    protected void handleStateEntered(Behaviour state) {
        super.handleStateEntered(state);
        LoggingUtils.logFsmState(this, this.getCurrent());
    }

    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
        LoggingUtils.logReceiveMsg(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), cfp);
        ACLMessage msg = createReply(cfp, ACLMessage.PROPOSE, JsonUtils.toJson(locationSupplier.get()));
        msg.setReplyByDate(Date.from(Instant.now().plusSeconds(10)));
        return msg;
    }

    protected void notifyObserver(ACLMessage cfp, ACLMessage proposal, ACLMessage accepted, ACLMessage rejected) {
        result = new Result(cfp, proposal, accepted, rejected);
    }

    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
        LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "Accept proposal from " + accept.getSender().getName());
        notifyObserver(cfp, propose, accept, null);
        return MessageUtils.createReply(accept, ACLMessage.INFORM, null);
    }

    @Override
    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        notifyObserver(cfp, propose, null, reject);
        if (reject == null || reject.getSender() == null) {
            LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "REJECT!! " + cfp.getSender().getName());
            return;
        }
        LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "Reject proposal from " + reject.getSender().getName());
    }
}
