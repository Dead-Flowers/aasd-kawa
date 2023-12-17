package pl.smartbin;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class MainApplication {
    public static void main(String[] args) {
        // -gui -name TEST
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        profile.setParameter(Profile.CONTAINER_NAME, "MyContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.PLATFORM_ID, "MyPlatform");
        AgentContainer container = runtime.createMainContainer(profile);

        for (int i = 1; i < 2; i++) {
            try {
                container.createNewAgent("GarbageCollector " + i, "pl.smartbin.GarbageCollectorAgent", null)
                         .start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i < 3; i++) {
            try {
                container.createNewAgent("Bin " + i, "pl.smartbin.BinAgent", null)
                         .start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i < 2; i++) {
            try {
                container.createNewAgent("Beacon " + i, "pl.smartbin.BeaconAgent", null)
                         .start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
    }
}

