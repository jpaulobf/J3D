package j3d.core;

import j3d.render.IRenderer;
import j3d.render.SoftwareRenderer;
import j3d.render.OpenGLRenderer;
import j3d.lighting.PointLight;
import j3d.physics.PhysicsEngine;
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
public class Game implements Runnable {

    // Window resolution constants
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;

    // Game state variables
    private boolean running = true;
    private boolean wireframe = false;
    private boolean showLightGizmo = false;

    // Game components
    private IGameWindow window;
    private IRenderer renderer;
    private InputManager input;
    private Robot robot;
    private Camera camera;
    private List<GameObject> objects;
    private List<PointLight> lights;
    private GameObject lightGizmo;
    private List<GameObject> gizmoList;
    private PhysicsEngine physics;

    // UI / HUD
    private HUD hud;

    // FPS Control
    private int TARGET_FPS = 60;
    private int fps = 0;
    private int windowCenterX = WIDTH / 2;
    private int windowCenterY = HEIGHT / 2;

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
        // Game initialization
        // Select Renderer Here:
        // renderer = new SoftwareRenderer(WIDTH, HEIGHT);
        renderer = new OpenGLRenderer(WIDTH, HEIGHT);

        // Factory: Create Window based on Renderer type
        if (renderer instanceof OpenGLRenderer) {
            window = new LwjglWindow("Engine 3D - OpenGL", WIDTH, HEIGHT);
            ((LwjglWindow) window).captureCursor(); // Hides and locks cursor for FPS view
        } else {
            window = new Window("Engine 3D - Software", WIDTH, HEIGHT);
        }

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

        // Release the OpenGL context from the Main thread so the Game thread can claim
        // it later
        window.releaseContext();

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
    private void update(double deltaTime) {
        // Speed correction based on FPS to ensure consistent movement
        double speedCorrection = deltaTime * 60.0;

        // Toggle rendering and visualization modes
        if (isKeyPressed(KeyEvent.VK_F2))
            wireframe = !wireframe;
        if (isKeyPressed(KeyEvent.VK_F3))
            showLightGizmo = !showLightGizmo;
        if (isKeyPressed(KeyEvent.VK_F4))
            GameObject.gouraud = !GameObject.gouraud;

        if (isKeyPressed(KeyEvent.VK_F5)) {
            // Assuming you cast if your renderer variable is an interface
            if (renderer instanceof SoftwareRenderer) {
                SoftwareRenderer sr = (SoftwareRenderer) renderer;
                sr.ssaaEnabled = !sr.ssaaEnabled;
                System.out.println("SSAA 2x: " + (sr.ssaaEnabled ? "ON" : "OFF"));
            }
        }

        if (isKeyPressed(KeyEvent.VK_F6))
            hud.setVisible(!hud.isVisible());

        if (isKeyPressed(KeyEvent.VK_F10)) {
            GameObject.scanline = !GameObject.scanline;
            System.out.println("Scanline Rasterization: " + (GameObject.scanline ? "ON" : "OFF"));
        }

        if (isKeyPressed(KeyEvent.VK_ESCAPE))
            System.exit(0);

        // Camera movement with mouse
        if (window.isFocused()) {

            int dx = 0;
            int dy = 0;

            if (window instanceof LwjglWindow) {
                Point delta = ((LwjglWindow) window).getMouseDelta();
                dx = delta.x;
                dy = delta.y;
            } else {
                // Calculates mouse displacement from window center (Legacy AWT)
                dx = input.getMouseX() - windowCenterX;
                dy = input.getMouseY() - windowCenterY;
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

        // --- VERTICAL PHYSICS (Gravity and Jump) ---

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
        if (window instanceof LwjglWindow) {
            return ((LwjglWindow) window).isKeyDown(keyCode);
        }
        return input.isKeyHeld(keyCode);
    }

    private boolean isKeyPressed(int keyCode) {
        if (window instanceof LwjglWindow) {
            return ((LwjglWindow) window).isKeyPressedOnce(keyCode);
        }
        return input.isKeyPressed(keyCode);
    }

    private boolean isSprintActive() {
        try {
            if (window instanceof LwjglWindow) {
                // GLFW: Use Shift as sprint (mapped from VK_CAPS_LOCK in LwjglWindow)
                return ((LwjglWindow) window).isKeyDown(KeyEvent.VK_CAPS_LOCK);
            }
            // AWT: Use Caps Lock State
            return Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (Exception e) {
            return false;
        }
    }
    // ---------------------------------------------------

    @Override
    public void run() {
        // Claim the OpenGL context on the rendering thread (Game Thread)
        window.makeContextCurrent();

        // Initialize renderer resources (Buffers, OpenGL Capabilities, etc.)
        // Must be called on the thread that owns the context
        renderer.init();

        // Fixed time step for logic updates
        // For 60 FPS, this is approximately 16.66ms
        final double nsPerTick = 1000000000.0 / TARGET_FPS;

        long lastTime = System.nanoTime();
        double delta = 0;

        long timer = System.currentTimeMillis();
        int framesCount = 0; // Local variable to count frames

        while (running && !window.shouldClose()) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            boolean shouldRender = false;

            // Logic update loop (Physics/Game Logic)
            // Ensures the game runs at the correct speed regardless of framerate
            // If the PC lags, this while loop runs multiple times to catch up to real time
            while (delta >= 1) {
                update(1.0 / TARGET_FPS); // Pass a constant and stable delta time
                delta--;
                shouldRender = true;
            }

            if (shouldRender) {
                renderer.clear();
                renderer.draw(camera, objects, lights, wireframe);
                if (showLightGizmo) {
                    renderer.draw(camera, gizmoList, null, true);
                }
                hud.draw(renderer, WIDTH, HEIGHT, fps);
                window.update(renderer.getFrameBuffer());
                framesCount++;
            } else {
                // If no update or render needed, sleep a bit to save CPU
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }

            // FPS counter update (1 second)
            if (System.currentTimeMillis() - timer >= 1000) {
                fps = framesCount;
                framesCount = 0;
                timer += 1000;

                // Recalculates window center (in case user moves it)
                try {
                    if (window.isFocused()) {
                        Point loc = window.getLocationOnScreen();
                        windowCenterX = loc.x + window.getWidth() / 2;
                        windowCenterY = loc.y + window.getHeight() / 2;
                    }
                } catch (Exception e) {
                }
            }
        }
        window.destroy();
    }
}