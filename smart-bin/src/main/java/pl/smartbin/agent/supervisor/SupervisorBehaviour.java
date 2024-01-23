package pl.smartbin.agent.supervisor;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import pl.smartbin.AgentType;
import pl.smartbin.MessageProtocol;
import pl.smartbin.dto.BinData;
import pl.smartbin.dto.Location;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.MessageUtils;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static pl.smartbin.utils.LoggingUtils.*;

public class SupervisorBehaviour extends FSMBehaviour {

    static final String CHECK_ENV = "check-env";
    static final String RECEIVE_CAPACITIES = "receive-capacities";
    static final String PROCESS_AUCTION = "process-auction";
    static final String FINAL = "final";

    private final SupervisorAgent agent;

    private HashMap<AID, BinData> binCapacities = new HashMap<>();
    private AID[] gcAIDs = null;

    public SupervisorBehaviour(SupervisorAgent a) {
        super(a);

        this.agent = a;

        registerDefaultTransition(CHECK_ENV, RECEIVE_CAPACITIES);
        registerTransition(CHECK_ENV, FINAL, 0);
        registerDefaultTransition(RECEIVE_CAPACITIES, PROCESS_AUCTION);
        registerTransition(RECEIVE_CAPACITIES, FINAL, 0);
        registerDefaultTransition(PROCESS_AUCTION, FINAL);


        Behaviour b;

        b = new OneShotBehaviour(a) {
            @Override
            public void action() {
            }

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


        b = new GarbageCollectionAuctionInitiator(a, this::getCfp, () -> binCapacities);
        registerState(b, PROCESS_AUCTION);
        b.reset();
        b = new OneShotBehaviour(a) {
            public void action() {

            }
        };
        registerLastState(b, FINAL);
    }

    @Override
    protected void handleStateEntered(Behaviour state) {
        super.handleStateEntered(state);
        logFsmState(this, state);
    }

    private HashMap<AID, BinData> receiveBinCapacities() {
        myAgent.send(MessageUtils.createMessage(ACLMessage.QUERY_IF, MessageProtocol.Supervisor2Beacon_Capacities,
                agent.getBeaconAID()));
        logSendMsg(AgentType.SUPERVISOR, agent.getName(), agent.getBeaconAID().getName());
        ACLMessage msg = agent.blockingReceive(MessageTemplate.MatchProtocol(MessageProtocol.Supervisor2Beacon_Capacities));
        logReceiveMsg(AgentType.SUPERVISOR, agent.getName(), msg);
        return JsonUtils.fromJson(msg.getContent(), new TypeToken<HashMap<AID, BinData>>() {
        }.getType());
    }

    private boolean shouldStartAuction() {
        return binCapacities.values().stream().allMatch(bc -> bc.usedCapacityPct > 50) || binCapacities.values().stream().anyMatch(bc -> bc.usedCapacityPct >= 95);
    }

    private ACLMessage getCfp() {
        gcAIDs = AgentUtils.findGarbageCollectors(agent, AgentType.SUPERVISOR);
        log(AgentType.SUPERVISOR, agent.getName(), "found %s garbage collectors".formatted(gcAIDs.length));
        ACLMessage cfp = MessageUtils.createMessage(ACLMessage.CFP, FIPANames.InteractionProtocol.FIPA_CONTRACT_NET, gcAIDs);
        cfp.setReplyByDate(Date.from(Instant.now().plusSeconds(7)));
        cfp.setContent(JsonUtils.toJson(binCapacities));

        return cfp;
    }

    private AID getWinnerAID() {
        return gcAIDs[0];
    }
}