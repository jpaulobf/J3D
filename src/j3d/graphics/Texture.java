package j3d.graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Texture class responsible for loading and sampling image textures for 3D
 * objects.
 */
public class Texture {

    private int width;
    private int height;
    private int[] pixels;
    public String path;

    /**
     * Constructor to create a texture from a file path.
     * 
     * @param path The file path of the image.
     */
    public Texture(String path) {
        this.path = path;
        load();
    }

    /**
     * Loads the image file and converts it to a pixel array.
     */
    private void load() {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            width = image.getWidth();
            height = image.getHeight();
            pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            System.out.println("Textura carregada: " + path);
        } catch (IOException e) {
            System.err.println("Erro ao carregar textura: " + path);
            // Creates error texture (Magenta/Black)
            createErrorTexture();
        }
    }

    /**
     * Creates a default error texture (Magenta/Black) to indicate missing
     * resources.
     */
    private void createErrorTexture() {
        width = 2;
        height = 2;
        pixels = new int[] { 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFFFF00FF };
    }

    /**
     * Samples a color from the texture at specific UV coordinates.
     * 
     * @param u Horizontal coordinate (0.0 to 1.0)
     * @param v Vertical coordinate (0.0 to 1.0)
     * @return The integer color value (ARGB)
     */
    public int getSample(double u, double v) {
        // Wrap (repeat)
        u -= Math.floor(u);
        v -= Math.floor(v);

        // Invert V (Image coordinates vs UV)
        v = 1.0 - v;

        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));

        return pixels[y * width + x];
    }
}
