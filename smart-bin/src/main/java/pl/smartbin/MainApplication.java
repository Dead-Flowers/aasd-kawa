package pl.smartbin;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.*;
import pl.smartbin.agent.supervisor.SupervisorAgent;
import pl.smartbin.dto.Location;
import pl.smartbin.utils.LocationUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static pl.smartbin.utils.LocationUtils.getRandomLocation;

public class MainApplication {
    private static ContainerController container;

    public static void main(String[] args) throws IOException, ControllerException {
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
            Object[] agentArgs = new Object[]{getRandomLocation()};
            gui.createGarbageCollector(
                "GarbageCollector " + i,
                agentArgs
            );
        }
        var locations = List.of(new Location(25, 50), new Location(75, 50));
        for (int i = 0; i < 2; i++) {
            Object[] agentArgs = new Object[]{Integer.toString(i), locations.get(i)};
            gui.createBeacon(
                    "Beacon " + i,
                    agentArgs
            );
            //agentArgs[1] = getRandomLocation();
            gui.createSupervisor(
                    "Supervisor " + i,
                    agentArgs
            );
        }

        for (int i = 0; i < 4; i++) {
            Object[] agentArgs = new Object[]{String.valueOf(i % 2), getRandomLocation()};
            gui.createBin(
                "Bin " + i,
                agentArgs
            );
        }


    }
}

