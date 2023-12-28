package pl.smartbin.agent.supervisor;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import pl.smartbin.AgentType;
import pl.smartbin.MessageProtocol;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pl.smartbin.utils.LoggingUtils.*;
import static pl.smartbin.utils.LoggingUtils.log;

public class SupervisorBehaviour extends FSMBehaviour {

    static final String CHECK_ENV = "check-env";
    static final String SEND_MESS = "send-mess";
    static final String RECEIVE_CAPACITIES = "receive-capacities";
    static final String PROCESS_AUCTION = "process-auction";
    static final String FINAL = "final";

    private final SupervisorAgent agent;

    private List<Integer> binCapacities = null;
    private AID[] gcAIDs = null;

    private Runnable beforeEnd;

    public SupervisorBehaviour(SupervisorAgent a) {
        super(a);

        this.agent = a;

        registerDefaultTransition(CHECK_ENV, SEND_MESS);
        registerTransition(CHECK_ENV, FINAL, 0);
        registerDefaultTransition(SEND_MESS, RECEIVE_CAPACITIES);
        registerDefaultTransition(RECEIVE_CAPACITIES, PROCESS_AUCTION);
        registerTransition(RECEIVE_CAPACITIES, FINAL, 0);
        registerDefaultTransition(PROCESS_AUCTION, FINAL);


        Behaviour b;

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {}

            @Override
            public int onEnd() {
                if (agent.getBeaconAID() == null) {
                    System.out.println("BeaconID " + agent.getBeaconAID().getName());
                    return 0;
                }
                return 1;
            }
        };
        registerFirstState(b, CHECK_ENV);

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {
                sendQuery();
            }
        };
        registerState(b, SEND_MESS);

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {
                binCapacities = receiveBinCapacities();
            }

            @Override
            public int onEnd() {
                if (binCapacities.isEmpty() || !shouldStartAuction()) {
                    return 0;
                }
                return 1;
            }
        };
        registerState(b, RECEIVE_CAPACITIES);


        b = new GarbageCollectionAuctionInitiator(a, getCfp(), () -> binCapacities);
        registerState(b, PROCESS_AUCTION);

        b = new OneShotBehaviour(a) {
            public void action() {
                if (beforeEnd != null) {
                    System.out.println("Running before end");
                    beforeEnd.run();
                }
            }
        };
        registerLastState(b, FINAL);
    }

    public SupervisorBehaviour onBeforeEnd(Runnable runnable) {
        beforeEnd = runnable;
        return this;
    }

    private void sendQuery() {
        myAgent.send(MessageUtils.createMessage(ACLMessage.QUERY_IF, MessageProtocol.Supervisor2Beacon_Capacities,
                                                agent.getBeaconAID()));
        logSendMsg(AgentType.SUPERVISOR, agent.getName(), agent.getBeaconAID().getName());
    }

    private List<Integer> receiveBinCapacities() {
        ACLMessage msg = agent.blockingReceive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2Beacon_Capacities));
        logReceiveMsg(AgentType.SUPERVISOR, agent.getName(), msg);

        List<Integer> binCapacities = JsonUtils.fromJson(msg.getContent(), new TypeToken<List<Integer>>() {}.getType());

        return binCapacities;
    }

    private boolean shouldStartAuction() {
        return binCapacities.stream().allMatch(bc -> bc > 50);
    }

    private ACLMessage getCfp() {
        gcAIDs = AgentUtils.findGarbageCollectors(agent, AgentType.SUPERVISOR);
        log(AgentType.SUPERVISOR, agent.getName(), "found %s garbage collectors".formatted(gcAIDs.length));
        ACLMessage cfp = MessageUtils.createMessage(ACLMessage.CFP, FIPANames.InteractionProtocol.FIPA_CONTRACT_NET, gcAIDs);
        cfp.setReplyByDate(Date.from(Instant.now().plusSeconds(7)));

        return cfp;
    }

    private AID getWinnerAID() {
        return gcAIDs[0];
    }
}