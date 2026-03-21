package j3d.graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Texture {

    private int width;
    private int height;
    private int[] pixels;
    public String path;

    public Texture(String path) {
        this.path = path;
        load();
    }

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
            // Cria textura de erro (Magenta/Preto)
            createErrorTexture();
        }
    }

    private void createErrorTexture() {
        width = 2;
        height = 2;
        pixels = new int[] { 0xFFFF00FF, 0xFF000000, 0xFF000000, 0xFFFF00FF };
    }

    public int getSample(double u, double v) {
        // Wrap (repetição)
        u -= Math.floor(u);
        v -= Math.floor(v);

        // Inverte V (Coordenadas de imagem vs UV)
        v = 1.0 - v;

        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));

        return pixels[y * width + x];
    }
}
