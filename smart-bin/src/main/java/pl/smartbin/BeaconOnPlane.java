package pl.smartbin;

import lombok.Getter;
import pl.smartbin.dto.Location;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Getter
public class BeaconOnPlane extends ObjectOnPlane {
    public static final int ICON_WIDTH = 60;
    public static final int ICON_HEIGHT = 60;

    private final Image image;

    public BeaconOnPlane(Location location, String name) throws IOException {
        super(location, name);
        BufferedImage fullRedImage = ImageIO.read(getClass().getResourceAsStream("/images/beacon.png"));
        image = fullRedImage.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST);
    }
}
