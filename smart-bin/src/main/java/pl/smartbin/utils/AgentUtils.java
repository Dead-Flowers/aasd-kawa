package pl.smartbin.utils;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import pl.smartbin.AgentType;

import java.util.Arrays;

public class AgentUtils {

    public static void registerAgent(Agent agent, AgentType agentType, Property... properties) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(agentType.getCode());
        sd.setName(agent.getAID().getName());

        Arrays.stream(properties)
                .forEach(sd::addProperties);

        dfd.addServices(sd);
        try {
            DFService.register(agent, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public static AID findBeacon(Agent caller, AgentType callerType, String region) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentType.BEACON.getCode());
        serviceDescription.addProperties(getRegionProp(region));
        template.addServices(serviceDescription);
        try {
            DFAgentDescription[] result = DFService.search(caller, template);
            System.out.printf("[%s %s] Found %d beacons\n", callerType.getCode(), caller.getName(), result.length);
            if (result.length == 1) {
                AID beaconAID = result[0].getName();
                System.out.printf("[%s %s] Found beacon %s\n", callerType.getCode(), caller.getName(),
                        beaconAID.getName());
                return beaconAID;
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static AID[] findGarbageCollectors(Agent caller, AgentType callerType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentType.GARBAGE_COLLECTOR.getCode());
        template.addServices(serviceDescription);
        try {
            DFAgentDescription[] result = DFService.search(caller, template);
            System.out.printf("[%s %s] Found %d garbage collectors\n", callerType.getCode(), caller.getName(), result.length);

            return Arrays.stream(result)
                    .map(DFAgentDescription::getName)
                    .toArray(AID[]::new);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return new AID[0];
    }

    public static AID[] findBins(Agent caller, AgentType callerType, String region) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(AgentType.BIN.getCode());
        serviceDescription.addProperties(getRegionProp(region));
        template.addServices(serviceDescription);
        try {
            DFAgentDescription[] result = DFService.search(caller, template);
            System.out.printf("[%s %s] Found %d bins\n", callerType.getCode(), caller.getName(), result.length);

            return Arrays.stream(result)
                    .map(DFAgentDescription::getName)
                    .toArray(AID[]::new);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Property getRegionProp(String region) {
        var prop = new Property();
        prop.setName("region_id");
        prop.setValue(region);

        return prop;
    }
}
