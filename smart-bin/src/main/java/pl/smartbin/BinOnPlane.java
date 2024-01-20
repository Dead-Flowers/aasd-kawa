package pl.smartbin;

import lombok.Getter;
import lombok.Setter;
import pl.smartbin.dto.Location;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

@Getter
public class BinOnPlane extends ObjectOnPlane{

    public static final int ICON_WIDTH = 40;
    public static final int ICON_HEIGHT = 40;

    @Setter
    private int usedCapacityPct;

    private final Image image;

    public BinOnPlane(Location location, String name) throws IOException {
        super(location, name);
        this.usedCapacityPct = 0;
        BufferedImage fullRedImage = ImageIO.read(getClass().getResourceAsStream("/images/bin.png"));
        image = fullRedImage.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST);
    }

    public BinOnPlane(Location location, String name, int usedCapacityPct) throws IOException {
        super(location, name);
        this.usedCapacityPct = usedCapacityPct;
        BufferedImage fullRedImage = ImageIO.read(getClass().getResourceAsStream("/images/bin.png"));
        image = fullRedImage.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST);
    }
}
