package j3d.core;

import j3d.physics.PhysicsEngine;
import j3d.lighting.LightController;
import j3d.player.PlayerController;
import j3d.enums.RenderType;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.io.ObjLoader;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import j3d.ui.HUD;

/**
 * Game class responsible for initializing the game, handling the main game
 * loop, processing user input, updating the game state, and rendering the
 * scene.
 */
public class Game extends AbstractGame {

    // Window resolution constants
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    // Game state variables
    private boolean wireframe = false;

    // Game components
    private Camera camera;
    private PhysicsEngine physics;
    private static final RenderType RENDER_TYPE = RenderType.OPENGL;
    private PlayerController playerController;
    private LightController lightController;

    // UI / HUD
    private HUD hud;

    /**
     * Game constructor, where we initialize the window, renderer, camera,
     * objects, and lights.
     */
    public Game() {
        super("Engine 3D", WIDTH, HEIGHT, RENDER_TYPE);
    }

    /**
     * Initialize the game.
     */
    @Override
    public void init() {
        // game objects
        input = new InputManager();
        camera = new Camera();
        objects = new ArrayList<>();
        physics = new PhysicsEngine();
        hud = new HUD(WIDTH, HEIGHT);

        // Hides the cursor
        if (window.getFrame() != null) {
            window.getFrame().setCursor(window.getFrame().getToolkit().createCustomCursor(
                    new BufferedImage(1, 1, 2), new Point(0, 0), ""));
        }

        // Initial scene object configuration
        this.getSceneInitialObjets();

        // Initial camera configuration
        this.initialSceneCameraConfiguration();

        // Input listener configuration
        // NOTE: Currently InputManager only supports AWT (Software Render).
        // OpenGL Input needs to be implemented via GLFW callbacks.
        window.addInputListener(input);

        // Ensures window/panel gets keyboard focus immediately upon starting
        window.requestFocus();

        // Initialize Player Controller
        playerController = new PlayerController(camera, input, window, physics);
        lightController = new LightController(camera, input, window);
    }

    /**
     * Sets up the initial camera position and orientation for a proper view of the
     * scene.
     */
    private void initialSceneCameraConfiguration() {
        // Initial camera configuration
        // Positions the player in the CENTER of the first room (Left Room)
        // Indices (1.5, 2.0) * blockSize (10.0)
        camera.transform.x = 30.0;
        camera.transform.z = 5.0;
        camera.transform.y = 4; // Eye height adjusted for new ceiling height

        // Validated orientation
        camera.yaw = 0;
        camera.pitch = 0;
    }

    /**
     * Sets up the initial scene objects, including basic shapes, an imported 3D
     * model, and a light.
     */
    private void getSceneInitialObjets() {
        // Scenery colors
        Color wallColor = new Color(245, 235, 205); // Beige / Creme claro
        Color platformColor = new Color(150, 100, 50); // Dark Wood
        Color stairColor = new Color(180, 120, 60); // Light Wood
        Color pillarColor = new Color(80, 80, 80);

        // 1. MAIN FLOOR (Level 0)
        // Creates a checkered grid floor
        GameObject floor = new GameObject(Mesh.createGrid(20, 5.0)); // 20x20 blocks of size 5 (Total 100x100)
        objects.add(floor);

        // 2. EXTERNAL WALLS (Arena)
        // High surrounding walls
        createBlock(-50, 10, 0, 1, 20, 50, wallColor); // Left Wall
        createBlock(50, 10, 0, 1, 20, 50, wallColor); // Right Wall
        createBlock(0, 10, 50, 50, 20, 1, wallColor); // Back Wall
        createBlock(0, 10, -50, 50, 20, 1, wallColor); // Front Wall

        // 3. MEZZANINE (Second Floor)
        // An elevated platform at Y=10, occupying the back of the room
        double mezaninoY = 10.0;
        createBlock(0, mezaninoY, 30, 30, 1, 15, platformColor);

        // Pillars
        createBlock(-25, 1.5, 40, 2, 10, 2, pillarColor);
        createBlock(25, 1.5, 40, 2, 10, 2, pillarColor);
        createBlock(-25, 1.5, 20, 2, 10, 2, pillarColor);
        createBlock(25, 1.5, 20, 2, 10, 2, pillarColor);

        // 4. CENTRAL STAIRCASE
        // Creates steps rising from floor to mezzanine
        double startZ = -3.0; // Starts a bit before mezzanine
        double startY = 0.0;
        int steps = 10;
        double stepHeight = 1.0;
        double stepDepth = 2.0;
        double stepWidth = 8.0;

        for (int i = 0; i < steps; i++) {
            // Each step goes up in Y and forward in Z
            double y = startY + (i * stepHeight);
            double z = startZ + (i * stepDepth);

            createBlock(0, y, z, stepWidth, stepHeight, stepDepth, stairColor).isMeshCollision = true;
        }

        // 5. UPPER WALKWAY (Bridge)
        // Connects mezzanine to a side balcony
        createBlock(-35, mezaninoY, 10, 5, 1, 20, platformColor); // Left side bridge
        createBlock(-45, mezaninoY, -10, 5, 1, 5, platformColor); // Small balcony

        // 6. Side Ramp
        double rampX = 18.0;
        double rampWidth = 5.0;
        double rampLength = 18.0;
        double rampStartZ = -3.0;
        double rampCenterZ = rampStartZ + (rampLength / 2.0);

        createRamp(rampX, mezaninoY / 1.66, rampCenterZ, rampWidth / 2.0, mezaninoY / 2.0, rampLength / 2.0,
                stairColor).isMeshCollision = true;

        // read 3D scene model, with texture and color

        // Uses loadScene to separate furniture/walls into individual collision objects
        List<GameObject> sceneObjects = ObjLoader.loadScene("res/sala.obj", Color.LIGHT_GRAY);
        for (GameObject obj : sceneObjects) {
            obj.transform.y = -0.5;
            obj.transform.x = -6;
            obj.transform.z = -25;
            obj.transform.setScale(20);
            objects.add(obj);
        }

        // ramp in 3d model
        List<GameObject> ramp = ObjLoader.loadScene("res/rampa.obj", Color.YELLOW);
        for (GameObject obj : ramp) {
            obj.transform.y = -0.5;
            obj.transform.x = 20;
            obj.transform.z = -25;
            // Aumentamos a escala pois o modelo rampa.obj é ~16x menor que os cubos nativos
            obj.transform.setScale(30); 
            objects.add(obj);
        }
    }

    /**
     * Updates game state, processing user input to move camera and light,
     * and updating object rotation. Also calculates current FPS and updates
     * window title with this information.
     */
    @Override
    public void input() {
        // Toggle rendering and visualization modes
        if (isKeyPressed(KeyEvent.VK_F2))
            wireframe = !wireframe;
        if (isKeyPressed(KeyEvent.VK_F4))
            GameObject.gouraud = !GameObject.gouraud;

        if (isKeyPressed(KeyEvent.VK_F5)) {
            renderer.toggleSsaa();
            System.out.println("SSAA 2x: " + (renderer.isSsaaEnabled() ? "ON" : "OFF"));
        }

        if (isKeyPressed(KeyEvent.VK_F6))
            hud.setVisible(!hud.isVisible());

        if (isKeyPressed(KeyEvent.VK_F10)) {
            GameObject.scanline = !GameObject.scanline;
            System.out.println("Scanline Rasterization: " + (GameObject.scanline ? "ON" : "OFF"));
        }

        if (isKeyPressed(KeyEvent.VK_F12))
            window.toggleFullscreen();

        if (isKeyPressed(KeyEvent.VK_ESCAPE))
            System.exit(0);
    }

    /**
     * Update the game.
     * 
     * @param deltaTime
     */
    @Override
    public void update(double deltaTime) {
        // Update Player Logic (Movement, Physics, Input)
        // Now the Game class doesn't need to know HOW the player moves, just that it
        // needs to update.
        playerController.update(deltaTime, objects);

        // Update Light Logic
        lightController.update(deltaTime);
    }

    /**
     * Render the scene.
     */
    @Override
    public void render() {
        renderer.clear();
        renderer.draw(camera, objects, lightController.getLights(), wireframe);
        if (lightController.isShowGizmos()) {
            renderer.draw(camera, lightController.getGizmos(), null, true);
        }
        hud.draw(renderer, WIDTH, HEIGHT, fps);
    }

    /**
     * Shutdown the game.
     */
    @Override
    public void shutdown() {
        // Release specific resources if needed
    }
}