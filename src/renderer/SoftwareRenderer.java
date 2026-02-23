package renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import camera.Camera;
import game.GameObject;
import geometry.Mesh;
import light.PointLight;

/**
 * SoftwareRenderer class implementing a basic 3D software renderer using Java
 * Swing.
 */
public class SoftwareRenderer extends JPanel implements Runnable, KeyListener, MouseMotionListener {

    // Constants for screen dimensions
    private static final int WIDTH = 800, HEIGHT = 600;
    private boolean running = true, wireframe = false, showLightGizmo = true;
    private Camera camera = new Camera();
    private List<GameObject> objects = new ArrayList<>();
    private List<PointLight> lights = new ArrayList<>();
    private GameObject lightGizmo;
    private boolean[] keys = new boolean[256];
    private double[] zBuffer = new double[WIDTH * HEIGHT];
    private Robot robot;
    private BufferedImage frameImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private int[] pixels = ((DataBufferInt) frameImage.getRaster().getDataBuffer()).getData();

    // VARIÁVEIS DE FPS ALVO
    private int TARGET_FPS = 240;
    private int fps = 0, frames = 0;
    private long lastFpsTime = System.currentTimeMillis();

    /**
     * Constructor for SoftwareRenderer.
     */
    public SoftwareRenderer() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        try {
            robot = new Robot();
        } catch (Exception e) {
        }
        setCursor(getToolkit().createCustomCursor(new BufferedImage(1, 1, 2), new Point(0, 0), ""));

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
    }

    /**
     * Update method to handle game logic and input.
     */
    private void update() {
        // Cálculo do fator de correção baseado em 60 FPS (16ms)
        double speedCorrection = 60.0 / TARGET_FPS;

        for (GameObject obj : objects) {
            if (obj != objects.get(2))
                obj.transform.rotY += 0.03 * speedCorrection;
        }

        PointLight spot = lights.get(0);
        double lSp = 0.3 * speedCorrection;
        if (keys[KeyEvent.VK_UP])
            spot.pos.z -= lSp;
        if (keys[KeyEvent.VK_DOWN])
            spot.pos.z += lSp;
        if (keys[KeyEvent.VK_LEFT])
            spot.pos.x -= lSp;
        if (keys[KeyEvent.VK_RIGHT])
            spot.pos.x += lSp;
        if (keys[KeyEvent.VK_I])
            spot.pos.y += lSp;
        if (keys[KeyEvent.VK_K])
            spot.pos.y -= lSp;

        // Atualiza a posição do gizmo da luz para coincidir com a posição da luz
        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

        double camSp = 0.3 * speedCorrection;
        double sY = Math.sin(camera.yaw), cY = Math.cos(camera.yaw);
        if (keys[KeyEvent.VK_W]) {
            camera.transform.x += sY * camSp;
            camera.transform.z -= cY * camSp;
        }
        if (keys[KeyEvent.VK_S]) {
            camera.transform.x -= sY * camSp;
            camera.transform.z += cY * camSp;
        }
        if (keys[KeyEvent.VK_A]) {
            camera.transform.x -= cY * camSp;
            camera.transform.z -= sY * camSp;
        }
        if (keys[KeyEvent.VK_D]) {
            camera.transform.x += cY * camSp;
            camera.transform.z += sY * camSp;
        }

        frames++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = System.currentTimeMillis();
        }
    }

    /**
     * Run method to start the rendering loop.
     */
    @Override
    public void run() {
        while (running) {
            update();
            repaint();
            try {
                // Cálculo dinâmico do tempo de espera
                Thread.sleep(1000 / TARGET_FPS);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Paint component method to render the scene.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Arrays.fill(pixels, 0);
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
        for (GameObject obj : objects)
            obj.draw(pixels, zBuffer, camera, lights, WIDTH, HEIGHT, wireframe);
        if (showLightGizmo)
            lightGizmo.draw(pixels, zBuffer, camera, null, WIDTH, HEIGHT, true);
        g.drawImage(frameImage, 0, 0, null);
        g.setColor(Color.WHITE);
        g.drawString("TARGET FPS: " + TARGET_FPS + " | ACTUAL FPS: " + fps, 10, 20);
    }

    /**
     * Mouse moved method to handle camera rotation based on mouse movement.
     */
    public void mouseMoved(MouseEvent e) {
        if (!isFocusOwner())
            return;
        Point c = getLocationOnScreen();
        camera.yaw += (e.getXOnScreen() - (c.x + WIDTH / 2)) * 0.003;
        camera.pitch += (e.getYOnScreen() - (c.y + HEIGHT / 2)) * 0.003;
        camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
        robot.mouseMove(c.x + WIDTH / 2, c.y + HEIGHT / 2);
    }

    /**
     * Mouse dragged method to handle mouse dragging (calls mouse moved).
     */
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Key pressed method to handle keyboard input for toggling wireframe mode,
     * light gizmo, and movement.
     */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F2)
            wireframe = !wireframe;
        if (e.getKeyCode() == KeyEvent.VK_F3)
            showLightGizmo = !showLightGizmo;
        if (e.getKeyCode() < 256)
            keys[e.getKeyCode()] = true;
    }

    /**
     * Key released method to handle keyboard input.
     */
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256)
            keys[e.getKeyCode()] = false;
    }

    public void keyTyped(KeyEvent e) {
    }
}