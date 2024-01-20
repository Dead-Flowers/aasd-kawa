package pl.smartbin;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import pl.smartbin.agent.garbage_collector.GarbageCollectorAgent;
import pl.smartbin.dto.Location;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class MainPlane extends JFrame {
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 800;

    private static MainPlane instance;
    private ContainerController container;

    private AgentPanel mainPanel;
    private StatsPanel statsPanel;

    private MainPlane(ContainerController container) {
        this.container = container;

//        this.generateGui();
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
        Location location = (Location) args[1];
        mainPanel.addBin(name, location);
    }

    public void createBeacon(String name, Object[] args) throws IOException {
        createAgent(name, BeaconAgent.class.getName(), args);
        Location location = (Location) args[1];
        mainPanel.addBeacon(name, location);
    }

    public void createGarbageCollector(String name, Object[] args) throws IOException {
        createAgent(name, GarbageCollectorAgent.class.getName(), args);
        Location location = (Location) args[0];
        mainPanel.addGarbageCollector(name, location);
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
        mainPanel.removeBin(name);
    }

    public void deleteGarbageCollectorAgent(String name) {
        this.deleteAgent(name);
        mainPanel.removeGarbageTruck(name);
    }

    public void deleteBeaconAgent(String name) {
        this.deleteAgent(name);
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


    public void updateBinFill(String binName, String currentBeaconName, int usedCapacityPct) {
        statsPanel.updateBinStat(binName, usedCapacityPct, currentBeaconName);
    }

    public void updateBeaconOnline(String beaconName) {
        statsPanel.updateBeaconSet(beaconName);
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
