package pl.smartbin;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.*;
import pl.smartbin.agent.supervisor.SupervisorAgent;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainApplication {
    private static ContainerController container;

    public static void main(String[] args) throws IOException {
        // -gui -name TEST
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        profile.setParameter(Profile.CONTAINER_NAME, "MyContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.PLATFORM_ID, "MyPlatform");
        container = runtime.createMainContainer(profile);

        MainPlane gui = MainPlane.getInstanceWithController(container);
        gui.generateGui();

        for (int i = 0; i < 2; i++) {
            gui.createGarbageCollector(
                "GarbageCollector " + i,
                null
            );
        }

        for (int i = 0; i < 4; i++) {
            gui.createBin(
                "Bin " + i,
                new Object[]{String.valueOf(i % 2)}
            );
        }

        for (int i = 0; i < 2; i++) {
            Object[] agentArgs = new Object[]{Integer.toString(i)};
            gui.createAgent(
                    "Beacon " + i,
                    "pl.smartbin.BeaconAgent",
                    agentArgs
            );
            gui.createAgent(
                    "Supervisor " + i,
                    SupervisorAgent.class.getName(),
                    agentArgs
            );
        }
    }
}

