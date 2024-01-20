package pl.smartbin;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AgentPanel extends JPanel {
    private final Map<String, BinOnPlane> bins;
    private final Map<String, GarbageCollectorOnPlane> trucks;

    public AgentPanel() {
        bins = new HashMap<>();
        trucks = new HashMap<>();
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
    }

    public void addBin(String binName, float latitude, float longitude) throws IOException {
        bins.put(binName, new BinOnPlane(latitude, longitude, binName));
        repaint();
    }

    public void removeBin(String binName) {
        bins.remove(binName);
        repaint();
    }

    public void addGarbageCollector(String truckName, float latitude, float longitude) throws IOException {
        trucks.put(truckName, new GarbageCollectorOnPlane(latitude, longitude, truckName));
        repaint();
    }

    public void removeGarbageTruck(String gcName) {
        trucks.remove(gcName);
        repaint();
    }

    public void updateBinUsedCapacity(String binName, int usedCapacityPct) {
        bins.get(binName).setUsedCapacityPct(usedCapacityPct);
        repaint();
    }

    private void drawPlane(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    private void drawBin(Graphics g, BinOnPlane bin) {
        int dotX = (int) (bin.getLongitude() / 100 * getWidth());
        int dotY = (int) (bin.getLatitude() / 100 * getHeight());

        g.setColor(Color.BLACK);
        Image binImage = bin.getImage();
        g.drawImage(binImage, dotX, dotY, null);
        g.drawString(bin.getName(), dotX - BinOnPlane.ICON_WIDTH/2, dotY + 10);
    }

    private void drawTruck(Graphics g, GarbageCollectorOnPlane truck) {
        int dotX = (int) (truck.getLongitude() / 100 * getWidth());
        int dotY = (int) (truck.getLatitude() / 100 * getHeight());

        g.setColor(Color.BLACK);
        Image truckImage = truck.getImage();
        g.drawImage(truckImage, dotX, dotY, null);
        g.drawString(truck.getName(), dotX - BinOnPlane.ICON_WIDTH/2, dotY + 10);
    }
}
