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
import pl.smartbin.AgentType;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.function.Supplier;

import static pl.smartbin.utils.MessageUtils.createReply;

public class GarbageCollectorBehaviour extends ContractNetResponder {

    public static final String COLLECT_GARBAGE = "Collect-Garbage";

    private final Supplier<GarbageCollector> locationSupplier;

    public GarbageCollectorBehaviour(Agent a, Supplier<GarbageCollector> locationSupplier) {
        super(a, createMessageTemplate(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET));
        this.locationSupplier = locationSupplier;

        deregisterDefaultTransition(HANDLE_ACCEPT_PROPOSAL);
        registerDefaultTransition(HANDLE_ACCEPT_PROPOSAL, COLLECT_GARBAGE);
        registerDefaultTransition(COLLECT_GARBAGE, SEND_REPLY);
        // latitmoże dodać jakieś wyjątkowe stany

        Behaviour b;

        b = new OneShotBehaviour() {

            @Override
            public void action() {
                // TODO prawdopodobnie wydzielić do osobnej klasy i dodać całe działanie wywozu śmieci
                LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, a.getName(), "Performing garbage collection");
            }
        };
        registerDSState(b, COLLECT_GARBAGE);
    }

    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
        LoggingUtils.logReceiveMsg(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), cfp);
        ACLMessage msg = createReply(cfp, ACLMessage.PROPOSE, JsonUtils.toJson(locationSupplier.get()));
//        msg.setReplyByDate(Date.from(Instant.now().plusSeconds(10)));
        return msg;
    }

    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
        LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "Accept proposal from " + accept.getSender().getName());
        return MessageUtils.createReply(accept, ACLMessage.INFORM, null);
    }

    @Override
    protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        if (reject == null || reject.getSender() == null) {
            LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "REJECT!! " + cfp.getSender().getName());
            return;
        }
        LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getName(), "Reject proposal from " + reject.getSender().getName());
    }
}
