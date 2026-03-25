package j3d.core;

import j3d.lighting.PointLight;
import j3d.physics.PhysicsEngine;
import j3d.enums.RenderType;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.io.ObjLoader;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
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
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    // Game state variables
    private boolean wireframe = false;
    private boolean showLightGizmo = false;

    // Game components
    private InputManager input;
    private Robot robot;
    private Camera camera;
    private List<GameObject> objects;
    private List<PointLight> lights;
    private GameObject lightGizmo;
    private List<GameObject> gizmoList;
    private PhysicsEngine physics;
    private static final RenderType RENDER_TYPE = RenderType.OPENGL;

    // UI / HUD
    private HUD hud;

    // Player Physics
    private double verticalVelocity = 0;
    private boolean isGrounded = false;
    private static final double GRAVITY = -25.0; // Gravity acceleration
    private static final double JUMP_FORCE = 10.0; // Jump force

    /**
     * Game constructor, where we initialize the window, renderer, camera,
     * objects, and lights.
     */
    public Game() {
        super("Engine 3D", WIDTH, HEIGHT, RENDER_TYPE);
    }

    @Override
    public void init() {
        //game objects
        input = new InputManager();
        camera = new Camera();
        objects = new ArrayList<>();
        lights = new ArrayList<>();
        gizmoList = new ArrayList<>();
        physics = new PhysicsEngine();
        hud = new HUD(WIDTH, HEIGHT);

        try {
            robot = new Robot();
        } catch (Exception e) {
        }

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
        if (window.getFrame() != null) {
            window.getFrame().addKeyListener(input);
            window.getFrame().addMouseMotionListener(input);
            window.getFrame().addMouseWheelListener(input);

            // Ensures window gets keyboard focus immediately upon starting
            window.getFrame().requestFocus();
        }
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
     * Helper to create solid blocks (Walls, Floors, Steps).
     * Uses standard cube and adjusts scale and position.
     */
    private void createBlock(double x, double y, double z, double sX, double sY, double sZ, Color color) {
        // Creates base cube
        Mesh m = Mesh.createCube();

        // Optional: Paint the cube a solid color to look better than standard rainbow
        if (color != null) {
            for (j3d.geometry.Triangle t : m.triangles) {
                t.baseColor = color;
            }
        }

        GameObject obj = new GameObject(m);
        obj.transform.x = x;
        obj.transform.y = y;
        obj.transform.z = z;
        obj.transform.scaleX = sX;
        obj.transform.scaleY = sY;
        obj.transform.scaleZ = sZ;

        objects.add(obj);
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

        // Mezzanine support pillars (Visual)
        createBlock(-25, 0, 40, 2, mezaninoY, 2, pillarColor);
        createBlock(25, 0, 40, 2, mezaninoY, 2, pillarColor);
        createBlock(-25, 0, 20, 2, mezaninoY, 2, pillarColor);
        createBlock(25, 0, 20, 2, mezaninoY, 2, pillarColor);

        // 4. CENTRAL STAIRCASE
        // Creates steps rising from floor to mezzanine
        double startZ = -3.0; // Starts a bit before mezzanine
        double startY = 0.0;
        int steps = 10;
        double stepHeight = mezaninoY / steps; // 1.0 height per step
        double stepDepth = 2.0;
        double stepWidth = 8.0;

        for (int i = 0; i < steps; i++) {
            // Each step goes up in Y and forward in Z
            double y = startY + (i * stepHeight);
            double z = startZ + (i * stepDepth);

            // The Y in createBlock is the object center.
            // If step has height 'stepHeight', center must be adjusted.
            createBlock(0, y, z, stepWidth, stepHeight, stepDepth, stairColor);
        }

        // 5. UPPER WALKWAY (Bridge)
        // Connects mezzanine to a side balcony
        createBlock(-35, mezaninoY, 10, 5, 1, 20, platformColor); // Left side bridge
        createBlock(-45, mezaninoY, -10, 5, 1, 5, platformColor); // Small balcony

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

        // Light configuration
        lights.add(new PointLight(0, 0, 0, Color.WHITE, 2)); // "Flashlight" Light
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));
        gizmoList.add(lightGizmo);
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
        if (isKeyPressed(KeyEvent.VK_F3))
            showLightGizmo = !showLightGizmo;
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

    @Override
    public void update(double deltaTime) {
        // Speed correction based on FPS to ensure consistent movement
        double speedCorrection = deltaTime * 60.0;

        // Camera movement with mouse
        if (window.isFocused()) {

            int dx = window.getMouseDeltaX(input.getMouseX(), windowCenterX);
            int dy = window.getMouseDeltaY(input.getMouseY(), windowCenterY);

            if (RENDER_TYPE != RenderType.OPENGL) {
                if (dx != 0 || dy != 0) {
                    robot.mouseMove(windowCenterX, windowCenterY);
                }
            }

            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003;
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
            }
        }

        // Applies gravity to vertical velocity
        verticalVelocity += GRAVITY * deltaTime;

        // Jump (only if grounded)
        if (isKeyPressed(KeyEvent.VK_SPACE) && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // Applies vertical velocity and resolves collision
        double dy = verticalVelocity * deltaTime;
        double nextY = camera.transform.y + dy;

        if (physics.checkPlayerCollision(camera.transform.x, nextY, camera.transform.z, objects)) {
            if (verticalVelocity < 0) {
                isGrounded = true; // Collided falling -> Floor
                verticalVelocity = 0;
            } else if (verticalVelocity > 0) {
                verticalVelocity = 0; // Collided rising -> Ceiling
            }
            // If collided, do not update Y (prevents clipping)
        } else {
            camera.transform.y = nextY;
            isGrounded = false; // Moved freely in Y, is in air (or falling)
        }

        // Camera movement with keyboard (WASD)
        double baseSpeed = 0.3;
        try {
            if (isSprintActive())
                baseSpeed *= 2;
        } catch (Exception e) {
        }

        double camSp = baseSpeed * speedCorrection;
        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);

        double dx = 0;
        double dz = 0;

        if (isKeyHeld(KeyEvent.VK_W)) {
            dx += sY * camSp;
            dz -= cY * camSp;
        }
        if (isKeyHeld(KeyEvent.VK_S)) {
            dx -= sY * camSp;
            dz += cY * camSp;
        }
        if (isKeyHeld(KeyEvent.VK_A)) {
            dx -= cY * camSp;
            dz -= sY * camSp;
        }
        if (isKeyHeld(KeyEvent.VK_D)) {
            dx += cY * camSp;
            dz += sY * camSp;
        }

        // Applies movement on X axis if no collision
        if (!physics.checkPlayerCollision(camera.transform.x + dx, camera.transform.y, camera.transform.z, objects)) {
            camera.transform.x += dx;
        }

        // Applies movement on Z axis if no collision (allows sliding on walls)
        if (!physics.checkPlayerCollision(camera.transform.x, camera.transform.y, camera.transform.z + dz, objects)) {
            camera.transform.z += dz;
        }

        // Light Logic (Flashlight)
        // Light follows camera position, but slightly ahead
        j3d.lighting.PointLight spot = lights.get(0);
        spot.pos.x = camera.transform.x;
        spot.pos.y = camera.transform.y;
        spot.pos.z = camera.transform.z;

        // Manual light controls (optional, kept for debug)
        double lSp = 0.3 * speedCorrection;
        if (isKeyHeld(KeyEvent.VK_U))
            spot.pos.z -= lSp;
        if (isKeyHeld(KeyEvent.VK_O))
            spot.pos.z += lSp;
        if (isKeyHeld(KeyEvent.VK_J))
            spot.pos.x -= lSp;
        if (isKeyHeld(KeyEvent.VK_L))
            spot.pos.x += lSp;
        if (isKeyHeld(KeyEvent.VK_I))
            spot.pos.y += lSp;
        if (isKeyHeld(KeyEvent.VK_K))
            spot.pos.y -= lSp;

        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

    }

    // --- INPUT HELPERS (Abstraction for AWT vs GLFW) ---
    private boolean isKeyHeld(int keyCode) {
        return window.isKeyDown(input, keyCode);
    }

    private boolean isKeyPressed(int keyCode) {
        return window.isKeyPressedOnce(input, keyCode);
    }

    private boolean isSprintActive() {
        return (window.isKeyDown(Toolkit.getDefaultToolkit(), KeyEvent.VK_CAPS_LOCK));
    }
    // ---------------------------------------------------

    @Override
    public void render() {
        renderer.clear();
        renderer.draw(camera, objects, lights, wireframe);
        if (showLightGizmo) {
            renderer.draw(camera, gizmoList, null, true);
        }
        hud.draw(renderer, WIDTH, HEIGHT, fps);
    }

    @Override
    public void shutdown() {
        // Release specific resources if needed
    }
}