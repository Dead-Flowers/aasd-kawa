package pl.smartbin;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import pl.smartbin.agent.garbage_collector.GarbageCollectorAgent;
import pl.smartbin.agent.garbage_collector.GarbageCollectorData;
import pl.smartbin.agent.supervisor.SupervisorAgent;
import pl.smartbin.dto.BinData;
import pl.smartbin.dto.Location;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPlane extends JFrame {
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 800;

    private static MainPlane instance;
    private ContainerController container;
    private List<String> garbageCollectors;
    private List<String> bins;
    private List<String> beacons;

    private AgentPanel mainPanel;
    private StatsPanel statsPanel;
    private final Timer timer;

    private MainPlane(ContainerController container) {
        this.container = container;
        this.garbageCollectors = new ArrayList<>();
        this.bins = new ArrayList<>();
        this.beacons = new ArrayList<>();
        timer = new Timer(500, e -> getDataFromAllAgents());
        timer.start();
    }

    private void getDataFromAllAgents() {
        Map<String, GarbageCollectorData> newGcData = new HashMap<>();
        for(String gcName: garbageCollectors) {
            try {
                AgentController agent = container.getAgent(gcName);
                var binAgentInterface = agent.getO2AInterface(IGarbageCollectorAgent.class);
                GarbageCollectorData data = binAgentInterface.getData();
                newGcData.put(gcName, data);
            } catch (ControllerException ex) {
                ex.printStackTrace();
            }
        }
        Map<String, BinData> newBinData = new HashMap<>();
        Map<String, String> beaconMapping = new HashMap<>();
        for(String binName: bins) {
            try {
                AgentController agent = container.getAgent(binName);
                var binAgentInterface = agent.getO2AInterface(IBinAgent.class);
                BinData data = binAgentInterface.getData();
                newBinData.put(binName, data);
                String beacon = binAgentInterface.getCurrentBeacon();
                beaconMapping.put(binName, beacon != null ? beacon : "");
            } catch (ControllerException ex) {
                ex.printStackTrace();
            }
        }
        Map<String, Location> newBeaconData = new HashMap<>();
        for(String beaconName: beacons) {
            try {
                AgentController agent = container.getAgent(beaconName);
                var beaconAgentInterface = agent.getO2AInterface(IBeaconAgent.class);
                Location data = beaconAgentInterface.getLocation();
                newBeaconData.put(beaconName, data);
            } catch (ControllerException ex) {
                ex.printStackTrace();
            }
        }
        try {
            updateDisplayedData(newGcData, newBinData, newBeaconData, beaconMapping);
        } catch (IOException ex) {

        }
    }

    public static MainPlane getInstance() {
        if (instance == null) {
            throw new RuntimeException("GUI not initialized");
        }
        return instance;
    }

    public static MainPlane getInstanceWithController(ContainerController container) {
        if (instance == null) {
            instance = new MainPlane(container);
        }
        return instance;
    }

    public void createBin(String name, Object[] args) throws IOException {
        createAgent(name, BinAgent.class.getName(), args);
        bins.add(name);
    }

    public void createBeacon(String name, Object[] args) throws IOException {
        createAgent(name, BeaconAgent.class.getName(), args);
        beacons.add(name);
    }

    public void createSupervisor(String name, Object[] args) throws IOException {
        createAgent(name, SupervisorAgent.class.getName(), args);
    }

    public void createGarbageCollector(String name, Object[] args) throws IOException {
        createAgent(name, GarbageCollectorAgent.class.getName(), args);
        garbageCollectors.add(name);
    }

    public void createAgent(String agentName, String agentClass, Object[] args) {
        try {
            AgentController ac = container.createNewAgent(agentName, agentClass, args);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public void deleteBinAgent(String name) {
        this.deleteAgent(name);
        bins.remove(name);
    }

    public void deleteGarbageCollectorAgent(String name) {
        this.deleteAgent(name);
        bins.remove(name);
    }

    public void deleteBeaconAgent(String name) {
        this.deleteAgent(name);
        beacons.remove(name);
    }

    public void deleteAgent(String agentName) {
        try {
            AgentController ac = container.getAgent(agentName);
            ac.kill();
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    public void addNewBin(int ordinalNo, Location location) throws IOException {
        Object[] agentArgs = new Object[]{String.valueOf(ordinalNo % 2), location};
        createBin("Bin " + ordinalNo, agentArgs);
    }

    public void updateDisplayedData(
            Map<String, GarbageCollectorData> newGcData,
            Map<String, BinData> newBinData,
            Map<String, Location> newBeaconData,
            Map<String, String> beaconMapping
    ) throws IOException {
        mainPanel.updateData(newGcData, newBinData, newBeaconData);
        statsPanel.updateAll(newBinData, newGcData, newBeaconData.keySet(), beaconMapping);
    }

    public void overrideBinUsedCapacity(String binName, int newValue) {
        try {
            AgentController agent = container.getAgent(binName);
            var binAgentInterface = agent.getO2AInterface(IBinAgent.class);
            binAgentInterface.overrideUsedCapacityPct(newValue);
        } catch (ControllerException ex) {
            ex.printStackTrace();
        }
    }

    public void generateGui() {
        setTitle("AASD - KAWA GUI");
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        mainPanel = new AgentPanel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        add(new JPanel(), BorderLayout.NORTH);

        add(new JPanel(), BorderLayout.SOUTH);
        add(new JPanel(), BorderLayout.WEST);

        statsPanel = new StatsPanel();
        JScrollPane scrollPane = new JScrollPane(statsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.EAST);

        pack();

        setVisible(true);
    }
}
