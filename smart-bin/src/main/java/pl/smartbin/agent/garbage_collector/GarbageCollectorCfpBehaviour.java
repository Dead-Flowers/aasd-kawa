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
import pl.smartbin.dto.Location;
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

    private final Supplier<Location> locationSupplier;

    public GarbageCollectorCfpBehaviour(Agent a, Supplier<Location> locationSupplier) {
        super(a, createMessageTemplate(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET));
        this.locationSupplier = locationSupplier;
        registerLastState(new OneShotBehaviour(a) {
            @Override
            public void action() {
                if (result.accepted != null) {
                    LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(),
                                     "Starting garbage collection for " + result.accepted.getSender().getLocalName());
                }
            }
        }, "Final");
        registerDefaultTransition(DUMMY_FINAL, "Final", new String[]{"Final"});
    }

    @Override
    public int onEnd() {
        return result != null && result.accepted != null ? 1 : 0;
    }


    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
        ACLMessage msg = createReply(cfp, ACLMessage.PROPOSE, JsonUtils.toJson(locationSupplier.get()));
        msg.setReplyByDate(Date.from(Instant.now().plusSeconds(10)));
        return msg;
    }

    protected void notifyObserver(ACLMessage cfp, ACLMessage proposal, ACLMessage accepted, ACLMessage rejected) {
        result = new Result(cfp, proposal, accepted, rejected);
    }

    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
        notifyObserver(cfp, propose, accept, null);
        return null;
    }

    @Override
    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        notifyObserver(cfp, propose, null, reject);
    }
}
