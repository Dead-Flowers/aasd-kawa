package pl.smartbin;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.Getter;
import lombok.Setter;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.MessageUtils;
import pl.smartbin.utils.AgentUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static pl.smartbin.utils.LoggingUtils.*;

public class SupervisorAgent extends Agent {

    private String region;

    @Getter
    private AID beaconAID;

    @Setter
    private boolean inProgress = false;

    @Override
    protected void setup() {
        System.out.println("Setting up '" + getAID().getName() + "'");

        this.region = (String) getArguments()[0];
        AgentUtils.registerAgent(this, AgentType.SUPERVISOR, AgentUtils.getRegionProp(region));

        var discoveryBh = new TickerBehaviour(this, TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick() {
                if (!inProgress) {
                    beaconAID = AgentUtils.findBeacon(myAgent, AgentType.SUPERVISOR, region);
                }
            }
        };

        var collectionStartBh = new TickerBehaviour(this, TimeUnit.SECONDS.toMillis(2)) {

            @Override
            protected void onTick() {
                if (beaconAID == null) {
                    return;
                }

                inProgress = true;
                addBehaviour(new SupervisorBehaviour((SupervisorAgent) myAgent));
            }
        };

        addBehaviour(discoveryBh);
        addBehaviour(collectionStartBh);
    }

    private static class SupervisorBehaviour extends FSMBehaviour {

        static final String CHECK_ENV = "check-env";
        static final String SEND_MESS = "send-mess";
        static final String RECEIVE_CAPACITIES = "receive-capacities";
        static final String START_AUCTION = "start-auction";
        static final String ACCEPT_OFFERS = "accept-offers";
        static final String SEND_RESPONSES = "send-responses";
        static final String WAIT_FOR_FINISH = "wait-for-finish";
        static final String FINAL = "final";

        private final SupervisorAgent agent;

        private List<Integer> binCapacities = null;
        private AID[] gcAIDs = null;
        private Long timeout = null;
        private List<Object> offers = new ArrayList<>();

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
                public void action() {
                }

                @Override
                public int onEnd() {
                    if (a.getBeaconAID() == null) {
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
//                    return offers.size();
                    return 1;
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
                    a.setInProgress(false);
                }
            };
            registerLastState(b, FINAL);
        }

        public void sendQuery() {
            myAgent.send(MessageUtils.createMessage(ACLMessage.QUERY_IF, MessageProtocol.Supervisor2Beacon_Capacities,
                                                    agent.getBeaconAID()));
            logSendMsg(AgentType.SUPERVISOR, agent.getName(), agent.getBeaconAID().getName());
        }

        public List<Integer> receiveBinCapacities() {
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
            agent.send(MessageUtils.createMessage(ACLMessage.CFP, MessageProtocol.Supervisor2GarbageCollector_Cfp, gcAIDs));
        }

        private void receiveOffer() {
            ACLMessage msg = agent.receive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2GarbageCollector_Offer));
            if (msg != null) {
                offers.add(msg.getSender());
            }
        }

        private void handleSendResponses() {
            AID winnerAID = getWinnerAID();
            log(AgentType.SUPERVISOR, agent.getName(), "Winner of the auction is %s".formatted(winnerAID.getName()));

            agent.send(MessageUtils.createMessage(ACLMessage.ACCEPT_PROPOSAL, MessageProtocol.Supervisor2GarbageCollector_Cfp, winnerAID));

            agent.send(MessageUtils.createMessage(ACLMessage.REJECT_PROPOSAL, MessageProtocol.Supervisor2GarbageCollector_Cfp,
                                                  Arrays.stream(gcAIDs)
                                                        .filter(aid -> !winnerAID.equals(aid))
                                                        .toArray(AID[]::new)));
        }

        private void receiveFinishMessage() {
//            ACLMessage msg = agent.blockingReceive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2GarbageCollector_Finish));
//            logReceiveMsg(AgentType.SUPERVISOR, agent.getName(), msg);
            log(AgentType.SUPERVISOR, agent.getName(), "Finished garbage collection");
        }

        private AID getWinnerAID() {
            return gcAIDs[0];
        }
    }
}
