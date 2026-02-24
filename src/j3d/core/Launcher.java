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

public class Launcher implements Runnable {

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

    public static void main(String[] args) {
        new Thread(new Launcher()).start();
    }

    public Launcher() {
        window = new Window("Engine 3D - Orientação Corrigida", WIDTH, HEIGHT);
        renderer = new SoftwareRenderer(WIDTH, HEIGHT);
        renderer.init();

        try {
            robot = new Robot();
        } catch (Exception e) {
        }
        
        window.getFrame().setCursor(window.getFrame().getToolkit().createCustomCursor(
            new BufferedImage(1, 1, 2), new Point(0, 0), ""));

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
        
        // --- AJUSTE PRECISO DE CÂMERA ---
        // Girar aprox -45 graus (-0.78 radianos) para sair do Leste e encarar o Norte
        camera.yaw = 2.3; 
        
        // Valor negativo para baixar a câmera (parar de olhar para cima)
        camera.pitch = 1.5; 
        // --------------------------------

        window.getFrame().addKeyListener(input);
        window.getFrame().addMouseMotionListener(input);
    }

    private void update() {
        double speedCorrection = 60.0 / TARGET_FPS;

        // 1. Toggles (F2, F3)
        if (input.isKeyPressed(KeyEvent.VK_F2)) wireframe = !wireframe;
        if (input.isKeyPressed(KeyEvent.VK_F3)) showLightGizmo = !showLightGizmo;

        // 2. Rotação da Câmera (Matemática da v7.5 restaurada)
        if (window.getFrame().isFocusOwner()) {
            java.awt.Point loc = window.getFrame().getLocationOnScreen();
            int centerX = loc.x + WIDTH / 2;
            int centerY = loc.y + HEIGHT / 2;

            // Calculamos o deslocamento em relação ao CENTRO da janela
            int dx = input.getMouseX() - centerX;
            int dy = input.getMouseY() - centerY;

            // Só aplicamos se houver movimento (evita drift)
            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003; 
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));

                // Reseta o mouse para o centro para o próximo frame
                robot.mouseMove(centerX, centerY);
            }
        }

        // 3. Movimento da Câmera (W, S, A, D)
        double camSp = 0.3 * speedCorrection;
        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);

        if (input.isKeyHeld(KeyEvent.VK_W)) { camera.transform.x += sY * camSp; camera.transform.z -= cY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_S)) { camera.transform.x -= sY * camSp; camera.transform.z += cY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_A)) { camera.transform.x -= cY * camSp; camera.transform.z -= sY * camSp; }
        if (input.isKeyHeld(KeyEvent.VK_D)) { camera.transform.x += cY * camSp; camera.transform.z += sY * camSp; }

        // 4. Movimento da Luz (Setas e I, K)
        j3d.lighting.PointLight spot = lights.get(0);
        double lSp = 0.3 * speedCorrection;
        if (input.isKeyHeld(KeyEvent.VK_UP))    spot.pos.z -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_DOWN))  spot.pos.z += lSp;
        if (input.isKeyHeld(KeyEvent.VK_LEFT))  spot.pos.x -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_RIGHT)) spot.pos.x += lSp;
        if (input.isKeyHeld(KeyEvent.VK_I))     spot.pos.y += lSp;
        if (input.isKeyHeld(KeyEvent.VK_K))     spot.pos.y -= lSp;

        // Sincroniza o Gizmo com a Luz
        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

        // 5. Rotação dos objetos
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
            } catch (Exception e) {
            }
        }
    }
}