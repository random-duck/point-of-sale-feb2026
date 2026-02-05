import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ImageUtils {

    // 1. Convert File -> Base64 String (for Saving)
    public static String encodeImage(File file) {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) return "";

            // Resize to thumbnail (max 300x300) to save space/speed
            int width = 300;
            int height = (int) (original.getHeight() * (300.0 / original.getWidth()));
            
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, width, height, null);
            g.dispose();

            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            // Convert to Base64 String
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 2. Convert Base64 String -> ImageIcon (for Displaying)
    public static ImageIcon decodeImage(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage img = ImageIO.read(bais);
            return new ImageIcon(img);
        } catch (Exception e) {
            return null; // Return null if broken
        }
    }
}