package pl.smartbin;

import pl.smartbin.agent.garbage_collector.GarbageCollector;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlaneWithGarbageTrucks extends JPanel {
    private final Map<String, GarbageCollector> pointsMap;

    public PlaneWithGarbageTrucks() {
        this.pointsMap = new HashMap<>();
        setSize(new Dimension(400, 400));
    }

    public void setPoint(String name, GarbageCollector point) {
        this.pointsMap.put(name, point);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawPlane(g);
        for (var entry : pointsMap.entrySet()) {
            drawDot(g, entry.getValue(), entry.getKey());
        }
    }

    private void drawPlane(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    private void drawDot(Graphics g, GarbageCollector collector, String label) {
        int dotX = (int)(collector.getLatitude() / 100 * getWidth());
        int dotY = (int)(collector.getLongitude() / 100 * getHeight());

        g.setColor(collector.getColor());
        g.fillOval(dotX - 5, dotY - 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawString(label, dotX + 10, dotY);
    }
}
