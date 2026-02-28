package j3d.render;

import java.util.List;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

/**
 * IRenderer interface defining the contract for rendering a 3D scene, including
 * initialization, clearing the frame buffer, and drawing the scene with given
 * camera, objects, and lights.
 */
public interface IRenderer {
    /**
     * Initializes the renderer, setting up necessary resources and configurations.
     */
    void init();

    /** Clears the frame buffer, preparing for a new frame to be drawn. */
    void clear();

    /** Método genérico de desenho que não conhece a lógica do cenário */
    void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe);

    /**
     * Returns the current frame buffer as an array of integers representing pixel colors.
     * @return
     */
    int[] getFrameBuffer();
}