package j3d.lighting;

import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.window.IGameWindow;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightning Controller class
 */
public class LightController {

    private List<PointLight> lights;
    private List<GameObject> gizmos;
    private GameObject lightGizmo;
    private boolean showGizmos = false;

    // Dependencies
    private Camera camera;
    private InputManager input;
    private IGameWindow window;

    /**
     * Constructor
     * 
     * @param camera
     * @param input
     * @param window
     */
    public LightController(Camera camera, InputManager input, IGameWindow window) {
        this.camera = camera;
        this.input = input;
        this.window = window;
        this.lights = new ArrayList<>();
        this.gizmos = new ArrayList<>();

        init();
    }

    /**
     * Init method
     */
    private void init() {
        // Default Light configuration (Flashlight style)
        PointLight mainLight = new PointLight(0, 0, 0, Color.WHITE, 2);
        lights.add(mainLight);

        // Gizmo configuration (Visual representation of the light source)
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));
        gizmos.add(lightGizmo);
    }

    /**
     * Update method
     * 
     * @param deltaTime
     */
    public void update(double deltaTime) {
        double speedCorrection = deltaTime * 60.0;

        // Input: Toggle Gizmo Visibility (F3)
        if (window.isKeyPressedOnce(input, KeyEvent.VK_F3)) {
            showGizmos = !showGizmos;
        }

        // Logic: Flashlight follows camera position
        PointLight spot = lights.get(0);
        spot.pos.x = camera.transform.x;
        spot.pos.y = camera.transform.y;
        spot.pos.z = camera.transform.z;

        // Input: Manual light controls (Debug/Offset)
        double lSp = 0.3 * speedCorrection;

        if (window.isKeyDown(input, KeyEvent.VK_U))
            spot.pos.z -= lSp;
        if (window.isKeyDown(input, KeyEvent.VK_O))
            spot.pos.z += lSp;
        if (window.isKeyDown(input, KeyEvent.VK_J))
            spot.pos.x -= lSp;
        if (window.isKeyDown(input, KeyEvent.VK_L))
            spot.pos.x += lSp;
        if (window.isKeyDown(input, KeyEvent.VK_I))
            spot.pos.y += lSp;
        if (window.isKeyDown(input, KeyEvent.VK_K))
            spot.pos.y -= lSp;

        // Update Gizmo position to match light
        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;
    }

    // Getters for Renderer
    public List<PointLight> getLights() {
        return lights;
    }

    public List<GameObject> getGizmos() {
        return gizmos;
    }

    public boolean isShowGizmos() {
        return showGizmos;
    }
}