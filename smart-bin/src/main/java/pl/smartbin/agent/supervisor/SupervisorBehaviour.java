package pl.smartbin.agent.supervisor;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import pl.smartbin.AgentType;
import pl.smartbin.MessageProtocol;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pl.smartbin.utils.LoggingUtils.*;
import static pl.smartbin.utils.LoggingUtils.log;

public class SupervisorBehaviour extends FSMBehaviour {

    static final String CHECK_ENV = "check-env";
    static final String SEND_MESS = "send-mess";
    static final String RECEIVE_CAPACITIES = "receive-capacities";
    static final String START_AUCTION = "start-auction";
    static final String ACCEPT_OFFERS = "accept-offers";
    static final String SEND_RESPONSES = "send-responses";
    static final String WAIT_FOR_FINISH = "wait-for-finish";
    static final String FINAL = "final";

    private final List<Object> offers = new ArrayList<>();
    private final SupervisorAgent agent;

    private List<Integer> binCapacities = null;
    private AID[] gcAIDs = null;
    private Long timeout = null;

    private Runnable beforeEnd;

    public SupervisorBehaviour(SupervisorAgent a) {
        super(a);

        this.agent = a;

        registerDefaultTransition(CHECK_ENV, SEND_MESS);
        registerTransition(CHECK_ENV, FINAL, 0);
        registerDefaultTransition(SEND_MESS, RECEIVE_CAPACITIES);
        registerDefaultTransition(RECEIVE_CAPACITIES, START_AUCTION);
        registerTransition(RECEIVE_CAPACITIES, FINAL, 0);
        registerDefaultTransition(START_AUCTION, ACCEPT_OFFERS);
        registerTransition(START_AUCTION, FINAL, 0);
        registerDefaultTransition(ACCEPT_OFFERS, SEND_RESPONSES);
        registerTransition(ACCEPT_OFFERS, FINAL, 0);
        registerDefaultTransition(SEND_RESPONSES, WAIT_FOR_FINISH);
        registerDefaultTransition(WAIT_FOR_FINISH,FINAL);


        Behaviour b;

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {}

            @Override
            public int onEnd() {
                if (agent.getBeaconAID() == null) {
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

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {
                sendCfp();
            }

            @Override
            public int onEnd() {
                return gcAIDs.length;
            }
        };
        registerState(b, START_AUCTION);

        b = new SimpleBehaviour(a) {
            @Override
            public void action() {
                if (timeout == null) {
                    timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
                }
                receiveOffer();
            }

            @Override
            public boolean done() {
                return offers.size() == gcAIDs.length || timeout - System.currentTimeMillis() < 0;
            }

            @Override
            public int onEnd() {
                LoggingUtils.log(AgentType.SUPERVISOR, agent.getName(),
                                 "was expecting %s offers, received %s".formatted(gcAIDs.length, offers.size()));
                return offers.size();
            }
        };
        registerState(b, ACCEPT_OFFERS);

        b = new OneShotBehaviour() {
            @Override
            public void action() {
                handleSendResponses();
            }
        };
        registerState(b, SEND_RESPONSES);

        b = new OneShotBehaviour() {
            @Override
            public void action() {
                receiveFinishMessage();
            }
        };
        registerState(b, WAIT_FOR_FINISH);

        b = new OneShotBehaviour(a) {
            public void action() {
                if (beforeEnd != null) {
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

    private void sendCfp() {
        gcAIDs = AgentUtils.findGarbageCollectors(agent, AgentType.SUPERVISOR);
        log(AgentType.SUPERVISOR, agent.getName(), "found %s garbage collectors".formatted(gcAIDs.length));
        agent.send(MessageUtils.createMessage(ACLMessage.CFP, MessageProtocol.Supervisor2GarbageCollector_Offer, gcAIDs));
    }

    private void receiveOffer() {
        ACLMessage msg = agent.receive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2GarbageCollector_Offer));
        if (msg != null) {
            logReceiveMsg(AgentType.SUPERVISOR, agent.getName(), msg);
            offers.add(msg.getSender());
        }
    }

    private void handleSendResponses() {
        AID winnerAID = getWinnerAID();
        log(AgentType.SUPERVISOR, agent.getName(), "Winner of the auction is %s".formatted(winnerAID.getName()));

        agent.send(MessageUtils.createMessage(ACLMessage.ACCEPT_PROPOSAL, MessageProtocol.Supervisor2GarbageCollector_Offer, winnerAID));

        agent.send(MessageUtils.createMessage(ACLMessage.REJECT_PROPOSAL, MessageProtocol.Supervisor2GarbageCollector_Offer,
                                              Arrays.stream(gcAIDs)
                                                    .filter(aid -> !winnerAID.equals(aid))
                                                    .toArray(AID[]::new)));
    }

    private void receiveFinishMessage() {
        ACLMessage msg = agent.blockingReceive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2GarbageCollector_Finish));
        logReceiveMsg(AgentType.SUPERVISOR, agent.getName(), msg);
        log(AgentType.SUPERVISOR, agent.getName(), "Finished garbage collection");
    }

    private AID getWinnerAID() {
        return gcAIDs[0];
    }
}