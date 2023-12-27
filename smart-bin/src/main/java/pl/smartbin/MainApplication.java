package pl.smartbin;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import pl.smartbin.agent.garbage_collector.GarbageCollector;
import pl.smartbin.agent.supervisor.SupervisorAgent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainApplication {

    private static final Map<AgentType, ArrayList<AgentController>> agents = new HashMap<>();

    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 800;
    private static JFrame frame;
    private static PlaneWithGarbageTrucks panel;
    private static Map<String, JLabel> binStates;
    private static AgentContainer container;

    public static void main(String[] args) {
        // -gui -name TEST
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        profile.setParameter(Profile.CONTAINER_NAME, "MyContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.PLATFORM_ID, "MyPlatform");
        container = runtime.createMainContainer(profile);

        for (int i = 1; i < 5; i++) {
            try {
                container.createNewAgent("GarbageCollector " + i, "pl.smartbin.agent.garbage_collector.GarbageCollectorAgent", null)
                         .start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        binStates = new HashMap<>();
        for (int i = 1; i < 5; i++) {
            try {
                var agent = container.createNewAgent("Bin " + i, "pl.smartbin.agent.bin.BinAgent", new Object[]{String.valueOf(i % 2)});
                agent.start();
                var label = new JLabel("Bin " + i + " 0%");
                label.setSize(200, 50);
                binStates.put("Bin " + i, label);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        for (Integer i = 0; i < 2; i++) {
            try {
                Object[] agentArgs = new Object[]{i.toString()};
                container.createNewAgent("Beacon " + i, "pl.smartbin.agent.beacon.BeaconAgent", agentArgs)
                         .start();
                container.createNewAgent("Supervisor " + i, SupervisorAgent.class.getName(), agentArgs)
                        .start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(MainApplication::startGUI);
    }

    private static void startGUI() {
        frame = new JFrame("AASD - KAWA GUI");
        panel = new PlaneWithGarbageTrucks();

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);

        frame.add(new JPanel(), BorderLayout.NORTH);

        frame.add(new JPanel(), BorderLayout.EAST);
        frame.add(new JPanel(), BorderLayout.WEST);
        Box box = Box.createVerticalBox();
        frame.add(box, BorderLayout.SOUTH);

        var binNames = binStates.keySet().stream().sorted().toList();

        for(var key : binNames) {
            box.add(binStates.get(key));
        }

        frame.pack();
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setVisible(true);
    }

    public static void updateBinState(String name, Integer value) {
        SwingUtilities.invokeLater(() -> binStates.get(name).setText(String.format("%s %s%%", name, value)));
    }

    public static void updateGarbageCollectorLocation(String name, GarbageCollector location) {
        panel.setPoint(name, location);
    }

}

