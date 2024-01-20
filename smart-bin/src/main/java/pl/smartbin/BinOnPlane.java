package pl.smartbin;

import lombok.Getter;
import lombok.Setter;

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

    public BinOnPlane(float latitude, float longitude, String name) throws IOException {
        super(latitude, longitude, name);
        this.usedCapacityPct = 0;
        BufferedImage fullRedImage = ImageIO.read(getClass().getResourceAsStream("/images/bin.png"));
        image = fullRedImage.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST);
    }
}
