package pl.smartbin;

import pl.smartbin.dto.Location;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AgentPanel extends JPanel {
    private final Map<String, BinOnPlane> bins;
    private final Map<String, GarbageCollectorOnPlane> trucks;
    private final Map<String, BeaconOnPlane> beacons;

    public AgentPanel() {
        bins = new HashMap<>();
        trucks = new HashMap<>();
        beacons = new HashMap<>();
        setPreferredSize(new Dimension(600, 400));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawPlane(g);
        for (var entry : bins.entrySet()) {
            drawBin(g, entry.getValue());
        }
        for (var entry : trucks.entrySet()) {
            drawTruck(g, entry.getValue());
        }
        for (var entry : beacons.entrySet()) {
            drawBeacon(g, entry.getValue());
        }
    }

    public void addBin(String binName, Location location) throws IOException {
        bins.put(binName, new BinOnPlane(location, binName));
        repaint();
    }

    public void removeBin(String binName) {
        bins.remove(binName);
        repaint();
    }

    public void addGarbageCollector(String truckName, Location location) throws IOException {
        trucks.put(truckName, new GarbageCollectorOnPlane(location, truckName));
        repaint();
    }

    public void removeGarbageTruck(String gcName) {
        trucks.remove(gcName);
        repaint();
    }

    public void addBeacon(String beaconName, Location location) throws IOException {
        beacons.put(beaconName, new BeaconOnPlane(location, beaconName));
        repaint();
    }

    public void removeBeacon(String beaconName) {
        beacons.remove(beaconName);
        repaint();
    }

    private void drawPlane(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    private void drawBin(Graphics g, BinOnPlane bin) {
        int dotX = (int) (bin.getLocation().longitude() / 100 * getWidth());
        int dotY = (int) (bin.getLocation().latitude() / 100 * getHeight());

        g.setColor(Color.BLACK);
        Image binImage = bin.getImage();
        g.drawImage(binImage, dotX, dotY, null);
        g.drawString(bin.getName(), dotX - BinOnPlane.ICON_WIDTH/2, dotY + 10);
    }

    private void drawTruck(Graphics g, GarbageCollectorOnPlane truck) {
        int dotX = (int) (truck.getLocation().longitude() / 100 * getWidth());
        int dotY = (int) (truck.getLocation().latitude() / 100 * getHeight());

        g.setColor(Color.BLACK);
        Image truckImage = truck.getImage();
        g.drawImage(truckImage, dotX, dotY, null);
        g.drawString(truck.getName(), dotX - BinOnPlane.ICON_WIDTH/2, dotY + 10);
    }

    private void drawBeacon(Graphics g, BeaconOnPlane beacon) {
        int dotX = (int) (beacon.getLocation().longitude() / 100 * getWidth());
        int dotY = (int) (beacon.getLocation().latitude() / 100 * getHeight());

        g.setColor(Color.BLACK);
        Image beaconImage = beacon.getImage();
        g.drawImage(beaconImage, dotX, dotY, null);
        g.drawString(beacon.getName(), dotX - BinOnPlane.ICON_WIDTH/2, dotY + 10);
    }
}
