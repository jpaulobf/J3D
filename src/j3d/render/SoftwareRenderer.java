package j3d.render;

import java.util.Arrays;
import java.util.List;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

/**
 * SoftwareRenderer class implementing the IRenderer interface, responsible for
 * rendering a 3D scene using software rendering techniques, including handling
 * frame buffer management and z-buffering for depth handling.
 */
public class SoftwareRenderer implements IRenderer {

    // Dimensions of the rendering area, frame buffer for pixel colors, and z-buffer for depth values
    private final int width, height;
    private int[] pixels;
    private double[] zBuffer;

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
    }

    /**
     * Clears the frame buffer, preparing for a new frame to be drawn.
     */
    @Override
    public void clear() {
        Arrays.fill(pixels, 0xFF87CEEB); // Fundo Azul (Céu)
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
    }

    /**
     * Método genérico de desenho que não conhece a lógica do cenário.
     */
    @Override
    public void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe) {
        if (objects == null)
            return;
        for (GameObject obj : objects) {
            // O Renderer apenas executa o comando de desenho nos buffers
            obj.draw(pixels, zBuffer, cam, lights, width, height, wireframe);
        }
    }

    /**
     * Returns the current frame buffer as an array of integers representing pixel colors.
     * 
     * @return
     */
    @Override
    public int[] getFrameBuffer() {
        return pixels;
    }
}