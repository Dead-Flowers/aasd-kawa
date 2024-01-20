package pl.smartbin.agent.garbage_collector;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ProposeInitiator;
import pl.smartbin.AgentType;
import pl.smartbin.IGarbageCollectorAgent;
import pl.smartbin.MessageProtocol;
import pl.smartbin.dto.BinData;
import pl.smartbin.dto.Location;
import pl.smartbin.utils.AgentUtils;
import pl.smartbin.utils.JsonUtils;
import pl.smartbin.utils.LoggingUtils;
import pl.smartbin.utils.MessageUtils;

import java.util.*;

import static pl.smartbin.utils.LocationUtils.getRandomLocation;
import static pl.smartbin.utils.MessageUtils.createMessage;

public class GarbageCollectorAgent extends Agent implements IGarbageCollectorAgent {

    public GarbageCollectorAgent() {
        super();
        this.registerO2AInterface(IGarbageCollectorAgent.class, this);
    }

    @Override
    public Location getCurrentLocation() {
        return state.getLocation();
    }

    @Override
    public GarbageCollectorData getData() {
        return state;
    }

    public static class States {
        public static final String INITIAL = "Initial";
        public static final String WAIT_SCHEDULE = "Wait-Schedule";
        public static final String BIN_PROPOSE = "Bin-propose";
        public static final String BIN_CLEAN = "Bin-clean";
        public static final String CFP = "Cfp";
        public static final String FINAL = "Finalee";
    }

    private final List<AID> beaconAgents = new ArrayList<>();
    private final DataStore cfpBhStore = new DataStore();
    private final DataStore binProposeStore = new DataStore();
    private GarbageCollectorData state;

    protected void setup() {
        state = new GarbageCollectorData((Location) this.getArguments()[0]);
        System.out.println("Setting up '" + getAID().getName() + "'");

        AgentUtils.registerAgent(this, AgentType.GARBAGE_COLLECTOR);

        var fsmBehavior = new FSMBehaviour(this);

        var gcBh = new GarbageCollectorCfpBehaviour(this, () -> state.getLocation());
        gcBh.setDataStore(cfpBhStore);

        var binProposeBh = new ProposeInitiator(this, null, binProposeStore) {

            HashMap<AID, ACLMessage> acceptedBins;
            HashMap<AID, BinData> allBins;

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            protected Vector prepareInitiations(ACLMessage propose) {
                acceptedBins = new HashMap<>();
                var msg = gcBh.getResult().cfp;
                allBins = JsonUtils.fromJson(msg.getContent(), new TypeToken<HashMap<AID, BinData>>() {
                }.getType());
                var bins = allBins.keySet().toArray(new AID[0]);
                propose = createMessage(ACLMessage.PROPOSE, FIPANames.InteractionProtocol.FIPA_PROPOSE, bins);
                //getDataStore().put(INITIATION_K, propose);
                return super.prepareInitiations(propose);
            }

            @Override
            protected void handleAcceptProposal(ACLMessage accept_proposal) {
                acceptedBins.put(accept_proposal.getSender(), accept_proposal);
            }
        };

        var binCleanBh = new SimpleBehaviour(this) {
            @Override
            public void action() {
                // TODO: simulate something..
                LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getLocalName(), "Got %s bins to clean left".formatted(binProposeBh.acceptedBins.size()));

                var binEntry = binProposeBh.acceptedBins.entrySet().stream().findFirst();
                binEntry.ifPresent(this::tryCleanBin);

                if (binProposeBh.acceptedBins.isEmpty()) {
                    LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getLocalName(), "Garbage collector used capacity after collection: " + state.getUsedCapacity());
                }
            }

            private void tryCleanBin(Map.Entry<AID, ACLMessage> binEntry) {
                int binUsedCapacity = Integer.parseInt(binEntry.getValue().getContent());
                int gcCapacity = binUsedCapacity / 10;

                if (state.hasSpace(gcCapacity)) {
                    state.addCapacity(gcCapacity);

                    var reply = MessageUtils.createReply(binEntry.getValue(), ACLMessage.INFORM, MessageProtocol.Bin2GarbageCollector, null);
                    send(reply);
                    LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getLocalName(), "Emptying bin: " + binEntry.getKey().getLocalName());
                    LoggingUtils.logSendMsg(AgentType.GARBAGE_COLLECTOR, reply);
                    binProposeBh.acceptedBins.remove(binEntry.getKey());
                } else {
                    LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getLocalName(), "No space left, cancelling garbage collection");
                    binProposeBh.acceptedBins.clear();
                }
            }

            @Override
            public boolean done() {
                return binProposeBh.acceptedBins.isEmpty();
            }
        };


        fsmBehavior.registerFirstState(new OneShotBehaviour(this) {
            @Override
            public void action() {

            }
        }, States.INITIAL);
        fsmBehavior.registerState(new WakerBehaviour(this, 5000) {
            public void onWake() {
                if (state.isFull()) {
                    LoggingUtils.log(AgentType.GARBAGE_COLLECTOR, myAgent.getLocalName(), "Removing trash from GC and setting new location");
                    state.clear();
                    state.setLocation(getRandomLocation());
                }
            }
        }, States.WAIT_SCHEDULE);

        var finalBh = new OneShotBehaviour(this) {
            @Override
            public void action() {
                // garbage collection completed, we should probably INFORM here
            }
        };

        var locationChangeBh = new TickerBehaviour(this, 1000) {
            @Override
            public void onTick() {
                var location = state.getLocation();
                state.setLocation(new Location(location.longitude()-1, location.latitude()-1));
            }
        };

        addBehaviour(locationChangeBh);

        fsmBehavior.registerState(gcBh, States.CFP);
        fsmBehavior.registerState(binProposeBh, States.BIN_PROPOSE);
        fsmBehavior.registerState(binCleanBh, States.BIN_CLEAN);
        fsmBehavior.registerState(finalBh, States.FINAL);

        fsmBehavior.registerDefaultTransition(States.INITIAL, States.WAIT_SCHEDULE);
        fsmBehavior.registerDefaultTransition(States.WAIT_SCHEDULE, States.CFP);
        fsmBehavior.registerTransition(States.CFP, States.BIN_PROPOSE, 1);
        fsmBehavior.registerTransition(States.CFP, States.WAIT_SCHEDULE, 0, new String[]{States.WAIT_SCHEDULE, States.CFP});
        fsmBehavior.registerDefaultTransition(States.BIN_PROPOSE, States.BIN_CLEAN);
        fsmBehavior.registerDefaultTransition(States.BIN_CLEAN, States.FINAL);
        fsmBehavior.registerDefaultTransition(States.FINAL, States.WAIT_SCHEDULE, new String[]{States.WAIT_SCHEDULE, States.CFP, States.BIN_PROPOSE, States.BIN_CLEAN, States.FINAL});


        addBehaviour(fsmBehavior);
    }


}
