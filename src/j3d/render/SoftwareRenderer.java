package j3d.render;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

/**
 * SoftwareRenderer class implementing the IRenderer interface, responsible for
 * rendering a 3D scene using software rendering techniques, including handling
 * frame buffer management and z-buffering for depth handling.
 */
public class SoftwareRenderer implements IRenderer {

    // Dimensions of the rendering area, frame buffer for pixel colors, and z-buffer
    // for depth values
    private final int width, height;
    private int[] pixels;
    private double[] zBuffer;

    // Buffers de Alta Resolução (2x) para o SSAA
    private int[] hrPixels;
    private double[] hrZBuffer;

    public boolean ssaaEnabled = false;

    /**
     * Constructor for SoftwareRenderer.
     * 
     * @param width
     * @param height
     */
    public SoftwareRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Initializes the renderer, setting up necessary resources and configurations.
     */
    @Override
    public void init() {
        this.pixels = new int[width * height];
        this.zBuffer = new double[width * height];

        // Inicializa os buffers gigantes (4 vezes mais pixels no total!)
        hrPixels = new int[(width * 2) * (height * 2)];
        hrZBuffer = new double[(width * 2) * (height * 2)];
    }

    /**
     * Clears the frame buffer, preparing for a new frame to be drawn.
     */
    @Override
    public void clear() {
        if (ssaaEnabled) {
            Arrays.fill(hrPixels, 0xFF87CEEB);
            Arrays.fill(hrZBuffer, 0.0);
        } else {
            Arrays.fill(pixels, 0xFF87CEEB);
            Arrays.fill(zBuffer, 0.0);
        }
    }

    /**
     * Método genérico de desenho que não conhece a lógica do cenário.
     */
    @Override
    public void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe) {
        if (objects == null)
            return;

        for (GameObject obj : objects) {
            if (ssaaEnabled) {
                // Manda o GameObject desenhar no buffer gigante (2x)
                obj.draw(hrPixels, hrZBuffer, cam, lights, width * 2, height * 2, wireframe);
            } else {
                // Desenho normal (1x)
                obj.draw(pixels, zBuffer, cam, lights, width, height, wireframe);
            }
        }
    }

    /**
     * Resolves the SSAA by averaging the colors of the 4 corresponding pixels in
     * the high-resolution buffer to produce the final color for each pixel in the
     * normal frame buffer.
     */
    private void resolveSSAA() {
        int hrWidth = width * 2;

        // Processamento paralelo para utilizar todos os núcleos da CPU
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowOffset = y * width;
            int hrRow1Offset = (y * 2) * hrWidth;
            int hrRow2Offset = hrRow1Offset + hrWidth;

            for (int x = 0; x < width; x++) {
                int hrX = x * 2;

                int p1 = hrPixels[hrRow1Offset + hrX];
                int p2 = hrPixels[hrRow1Offset + hrX + 1];
                int p3 = hrPixels[hrRow2Offset + hrX];
                int p4 = hrPixels[hrRow2Offset + hrX + 1];

                // Otimização Bitwise: Soma os canais mascarados e divide por 4 (>> 2) no final
                int b = (p1 & 0xFF) + (p2 & 0xFF) + (p3 & 0xFF) + (p4 & 0xFF);
                int g = (p1 & 0xFF00) + (p2 & 0xFF00) + (p3 & 0xFF00) + (p4 & 0xFF00);
                int r = (p1 & 0xFF0000) + (p2 & 0xFF0000) + (p3 & 0xFF0000) + (p4 & 0xFF0000);

                pixels[rowOffset + x] = ((r >> 2) & 0xFF0000) | ((g >> 2) & 0xFF00) | (b >> 2);
            }
        });
    }

    /**
     * Returns the current frame buffer as an array of integers representing pixel
     * colors.
     * 
     * @return
     */
    @Override
    public int[] getFrameBuffer() {
        if (ssaaEnabled) {
            resolveSSAA(); // Esmaga a imagem gigante antes de mandar para a tela
        }
        return pixels;
    }
}