package j3d.core;

import j3d.render.IRenderer;
import j3d.render.SoftwareRenderer;
import j3d.lighting.PointLight;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game implements Runnable {

    private static final int WIDTH = 800, HEIGHT = 600;
    private boolean running = true, wireframe = false, showLightGizmo = true;
    
    private Window window;
    private IRenderer renderer;
    private InputManager input = new InputManager();
    private Robot robot;

    private Camera camera = new Camera();
    private List<GameObject> objects = new ArrayList<>();
    private List<PointLight> lights = new ArrayList<>();
    private GameObject lightGizmo;

    private int TARGET_FPS = 240;
    private int fps = 0, frames = 0;
    private long lastFpsTime = System.currentTimeMillis();

    public Game() {
        window = new Window("Engine 3D - J3D Game", WIDTH, HEIGHT);
        renderer = new SoftwareRenderer(WIDTH, HEIGHT);
        renderer.init();

        try {
            robot = new Robot();
        } catch (Exception e) {}
        
        window.getFrame().setCursor(window.getFrame().getToolkit().createCustomCursor(
            new BufferedImage(1, 1, 2), new Point(0, 0), ""));

        // Setup da Cena
        GameObject cube = new GameObject(Mesh.createCube());
        cube.transform.x = -3;
        objects.add(cube);

        GameObject pyr = new GameObject(Mesh.createPyramid());
        pyr.transform.x = 3;
        objects.add(pyr);

        GameObject floor = new GameObject(Mesh.createGrid(20, 2.0));
        floor.transform.y = -1.5;
        objects.add(floor);

        lights.add(new PointLight(0, 5, 0, Color.WHITE, 1.5));
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));

        camera.transform.z = 15;
        camera.transform.y = 5;
        
        // Orientação validada
        camera.yaw = 2.3; 
        camera.pitch = 1.5; 

        window.getFrame().addKeyListener(input);
        window.getFrame().addMouseMotionListener(input);
    }

    private void update() {
        double speedCorrection = 60.0 / TARGET_FPS;

        if (input.isKeyPressed(KeyEvent.VK_F2)) wireframe = !wireframe;
        if (input.isKeyPressed(KeyEvent.VK_F3)) showLightGizmo = !showLightGizmo;

        if (window.getFrame().isFocusOwner()) {
            java.awt.Point loc = window.getFrame().getLocationOnScreen();
            int centerX = loc.x + WIDTH / 2;
            int centerY = loc.y + HEIGHT / 2;

            int dx = input.getMouseX() - centerX;
            int dy = input.getMouseY() - centerY;

            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003; 
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
                robot.mouseMove(centerX, centerY);
            }
        }

        double camSp = 0.3 * speedCorrection;
        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);

        if (input.isKeyHeld(KeyEvent.VK_W)) { camera.transform.x += sY * camSp; camera.transform.z -= cY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_S)) { camera.transform.x -= sY * camSp; camera.transform.z += cY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_A)) { camera.transform.x -= cY * camSp; camera.transform.z -= sY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_D)) { camera.transform.x += cY * camSp; camera.transform.z += sY * camSp; }

        j3d.lighting.PointLight spot = lights.get(0);
        double lSp = 0.3 * speedCorrection;
        if (input.isKeyHeld(KeyEvent.VK_UP))    spot.pos.z -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_DOWN))  spot.pos.z += lSp;
        if (input.isKeyHeld(KeyEvent.VK_LEFT))  spot.pos.x -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_RIGHT)) spot.pos.x += lSp;
        if (input.isKeyHeld(KeyEvent.VK_I))     spot.pos.y += lSp;
        if (input.isKeyHeld(KeyEvent.VK_K))     spot.pos.y -= lSp;

        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

        for (j3d.core.GameObject obj : objects) {
            if (obj != objects.get(2)) obj.transform.rotY += 0.03 * speedCorrection;
        }

        frames++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = System.currentTimeMillis();
            window.getFrame().setTitle("TARGET FPS: " + TARGET_FPS + " | ACTUAL FPS: " + fps);
        }
    }

    @Override
    public void run() {
        while (running) {
            update();
            renderer.clear();
            
            renderer.draw(camera, objects, lights, wireframe);
            if (showLightGizmo) {
                renderer.draw(camera, Arrays.asList(lightGizmo), null, true);
            }
            
            window.update(renderer.getFrameBuffer());
            
            try {
                Thread.sleep(1000 / TARGET_FPS);
            } catch (Exception e) {}
        }
    }
}