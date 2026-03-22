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

    /** Generic draw method that doesn't know scene logic */
    void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe);

    /**
     * Draws a 2D sprite directly into pixel buffer, ignoring Z-Buffer.
     * Useful for HUD, crosshairs, and interfaces.
     */
    void drawSprite(int[] spritePixels, int spriteW, int spriteH, int x, int y);

    /**
     * Returns the current frame buffer as an array of integers representing pixel
     * colors.
     * 
     * @return
     */
    int[] getFrameBuffer();

    boolean isSsaaEnabled();
}