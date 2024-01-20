package pl.smartbin;

import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Getter
public class GarbageCollectorOnPlane extends ObjectOnPlane {
    public static final int ICON_WIDTH = 80;
    public static final int ICON_HEIGHT = 80;

    @Setter
    private int usedCapacityPct;
    private final Image image;

    public GarbageCollectorOnPlane(float latitude, float longitude, String name) throws IOException {
        super(latitude, longitude, name);
        this.usedCapacityPct = 0;
        BufferedImage fullRedImage = ImageIO.read(getClass().getResourceAsStream("/images/truck.png"));
        image = fullRedImage.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST);
    }
}
