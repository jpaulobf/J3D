package j3d.render;

import java.util.Arrays;
import java.util.List;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

public class SoftwareRenderer implements IRenderer {
    private final int width, height;
    private int[] pixels;
    private double[] zBuffer;

    public SoftwareRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void init() {
        this.pixels = new int[width * height];
        this.zBuffer = new double[width * height];
    }

    @Override
    public void clear() {
        Arrays.fill(pixels, 0); // Fundo Preto
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
    }

    @Override
    public void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe) {
        if (objects == null) return;
        for (GameObject obj : objects) {
            // O Renderer apenas executa o comando de desenho nos buffers
            obj.draw(pixels, zBuffer, cam, lights, width, height, wireframe);
        }
    }

    @Override
    public int[] getFrameBuffer() {
        return pixels;
    }
}