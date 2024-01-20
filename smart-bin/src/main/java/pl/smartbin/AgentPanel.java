package pl.smartbin;

import pl.smartbin.dto.Location;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AgentPanel extends JPanel {

    record BinImageLocationData(int x, int y, int width, int height) {}

    private final Map<String, BinOnPlane> bins;
    private final Map<String, BinImageLocationData> binImageLocationDataMap;
    private final Map<String, GarbageCollectorOnPlane> trucks;
    private final Map<String, BeaconOnPlane> beacons;

    private MainPlane gui;

    public AgentPanel() {
        bins = new HashMap<>();
        trucks = new HashMap<>();
        beacons = new HashMap<>();
        binImageLocationDataMap = new HashMap<>();

        gui = MainPlane.getInstance();

        setPreferredSize(new Dimension(600, 400));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    handleMouseCLick(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        repaint();
    }

    private void handleMouseCLick(MouseEvent e) throws IOException {
        if (!e.isControlDown()) {
            checkIfBinPressed(e);
            return;
        }

        var ordinals = bins.keySet().stream().map(value -> Integer.parseInt(value.split(" ")[1])).toList();
        int lastBinNo = Collections.max(ordinals);
        int x = e.getX();
        int y = e.getY();

        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            gui.addNewBin(lastBinNo + 1, new Location(x*100f / getWidth(),  y*100f /  getHeight()));
        }
    }

    private void checkIfBinPressed(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        for(var binImgEntry : binImageLocationDataMap.entrySet()) {
            var v = binImgEntry.getValue();
            if (mouseX > v.x && mouseX < (v.x + v.width)) {
                if (mouseY > v.y && mouseY < (v.y + v.height)) {
                    // TODO: create popup to update
                    openDialog(binImgEntry.getKey());
                    break;
                }
            }
        }
    }

    private void openDialog(String binName) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Change bin utilization", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(150, 150);

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTextField textField = new JTextField(11);
        JPanel textFieldPanel = new JPanel();
        textFieldPanel.add(new JLabel("Updating " + binName));
        textFieldPanel.add(textField);
        dialog.add(textFieldPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            String inputText = textField.getText();
            try {
                int newValue = Integer.parseInt(inputText);
                if (newValue >= 0 && newValue <= 100) {
                    gui.overrideBinUsedCapacity(binName, newValue);
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {

            }

        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(SwingUtilities.getRoot(this));
        dialog.pack();
        dialog.setVisible(true);
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
        binImageLocationDataMap.remove(binName);
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
        binImageLocationDataMap.put(
                bin.name,
                new BinImageLocationData(
                        dotX,
                        dotY,
                        binImage.getWidth(this),
                        binImage.getHeight(this)
                )
        );
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
