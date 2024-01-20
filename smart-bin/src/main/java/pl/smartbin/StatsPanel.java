package pl.smartbin;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;

public class StatsPanel extends JPanel {

    record BeaconStat(int value, String beaconName) { }

    private final Map<String, BeaconStat> binStats;
    private final Map<String, Integer> garbageCollectorStats;
    private final Set<String> beaconsOnline;

    private MainPlane gui;

    public StatsPanel() {
        this.binStats = new HashMap<>();
        this.garbageCollectorStats = new HashMap<>();
        this.beaconsOnline = new HashSet<>();

        gui = MainPlane.getInstance();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(350, 300));
        regenerateTexts();
    }

    public void updateBinStat(String binName, int value, String beaconName) {
        binStats.put(binName, new BeaconStat(value, beaconName));
        regenerateTexts();
    }

    public void updateGcStat(String gcName, int value) {
        garbageCollectorStats.put(gcName, value);
        regenerateTexts();
    }

    public void updateBeaconSet(String beaconName) {
        beaconsOnline.add(beaconName);
        regenerateTexts();
    }

    public void deleteBin(String name) {
        binStats.remove(name);
        SwingUtilities.invokeLater(() -> gui.deleteBinAgent(name));
        regenerateTexts();
    }

    public void deleteBeacon(String name) {
        beaconsOnline.remove(name);
        SwingUtilities.invokeLater(() -> gui.deleteBeaconAgent(name));
        regenerateTexts();
    }

    public void deleteGc(String name) {
        garbageCollectorStats.remove(name);
        SwingUtilities.invokeLater(() -> gui.deleteGarbageCollectorAgent(name));
        regenerateTexts();
    }

    private void regenerateTexts() {
        clearTexts();
        JLabel beaconTitle = new JLabel("Beacons online");
        beaconTitle.setFont(new Font("Arial", Font.PLAIN, 18));
        add(beaconTitle);
        if (!beaconsOnline.isEmpty()) {
            var sortedBeacons = beaconsOnline.stream().sorted().toList();
            for(String key: sortedBeacons) {
//                JPanel rowPanel = new JPanel();
//                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
                JLabel label = new JLabel(key);
//                rowPanel.add(label);
                add(label);
            }
        } else {
            JLabel label = new JLabel("No bins");
            add(label);
        }
        JSeparator firstSep = new JSeparator();
        firstSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        add(firstSep);
        JLabel binTitle = new JLabel("Bins utilization");
        binTitle.setFont(new Font("Arial", Font.PLAIN, 18));
        add(binTitle);
        if (!binStats.isEmpty()) {
            var sortedBinKeys = binStats.keySet().stream().sorted().toList();
            for(String key: sortedBinKeys) {
//                JPanel rowPanel = new JPanel();
//                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
                var value = binStats.get(key);
                JLabel label = new JLabel(key + " (" + value.beaconName + "): " + value.value + "%");
//                rowPanel.add(label);
//                JButton removeButton = new JButton("Remove");
//                removeButton.addActionListener(e -> deleteBin(key));
//                rowPanel.add(removeButton);
                add(label);
            }
        } else {
            JLabel label = new JLabel("No bins");
            add(label);
        }
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        add(separator);
        JLabel gcTitle = new JLabel("Garbage trucks utilization");
        gcTitle.setFont(new Font("Arial", Font.PLAIN, 18));
        add(gcTitle);
        if(!garbageCollectorStats.isEmpty()) {
            var sortedGCKeys = garbageCollectorStats.keySet().stream().sorted().toList();
            for(String key: sortedGCKeys) {
//                JPanel rowPanel = new JPanel();
//                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
                JLabel label = new JLabel(key + ": " + garbageCollectorStats.get(key) + "%");
//                rowPanel.add(label);
//                JButton removeButton = new JButton("Remove");
//                removeButton.addActionListener(e -> deleteGc(key));
//                rowPanel.add(removeButton);
                add(label);
            }
        } else {
            JLabel label = new JLabel("No garbage trucks");
            add(label);
        }
        repaint();
    }

    private void clearTexts() {
        removeAll();
        revalidate();
    }


}
