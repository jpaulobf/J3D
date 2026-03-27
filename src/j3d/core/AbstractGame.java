package j3d.core;

import j3d.enums.RenderType;
import j3d.factory.RenderFactory;
import j3d.factory.WindowFactory;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.render.IRenderer;
import j3d.window.IGameWindow;
import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.List;

public abstract class AbstractGame implements Runnable {

    protected IGameWindow window;
    protected IRenderer renderer;
    protected boolean running = true;
    protected int width;
    protected int height;
    protected int targetFps = 120;
    protected List<GameObject> objects;
    protected InputManager input;

    // FPS Counters
    protected int fps = 0;
    protected int windowCenterX;
    protected int windowCenterY;

    public AbstractGame(String title, int width, int height, RenderType type) {
        this.width = width;
        this.height = height;
        this.windowCenterX = width / 2;
        this.windowCenterY = height / 2;

        // 1. Initialize Renderer and Window via Factories
        this.renderer = RenderFactory.createRenderer(type, width, height);
        this.window = WindowFactory.createWindow(type, title, width, height);

        // 2. Release context from Main Thread (critical for OpenGL)
        window.releaseContext();
    }

    @Override
    public void run() {
        // 3. Claim Context on Game Thread
        window.makeContextCurrent();

        // 4. Initialize Renderer (Buffers, GL Capabilities)
        renderer.init();

        // 5. Initialize Child Game Logic (Scene, Objects, etc)
        init();

        // 6. Game Loop
        final double nsPerTick = 1000000000.0 / targetFps;
        long lastTime = System.nanoTime();
        double delta = 0;
        long timer = System.currentTimeMillis();
        int framesCount = 0;

        while (running && !window.shouldClose()) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            // "Spiral of Death" protection
            if (delta > 10) delta = 10;

            boolean shouldRender = false;
            // 1. Fixed Update (UPS - Updates Per Second)
            while (delta >= 1) {
                input();
                update(1.0 / targetFps);
                delta--;
                shouldRender = true;
            }

            // 2. Capped Render: Only renders if a tick occurred
            if (shouldRender) {
                render();
                window.update(renderer.getFrameBuffer());
                framesCount++;
            } else {
                Thread.onSpinWait();
            }

            // FPS Timer
            if (System.currentTimeMillis() - timer >= 1000) {
                fps = framesCount;
                framesCount = 0;
                timer += 1000;
                
                // Recalculate center if window moved
                if (window.isFocused()) {
                    Point loc = window.getLocationOnScreen();
                    windowCenterX = loc.x + window.getWidth() / 2;
                    windowCenterY = loc.y + window.getHeight() / 2;
                }
            }
        }
        
        shutdown();
        window.destroy();
    }

    // --- Abstract Methods (To be implemented by Game) ---
    public abstract void init();
    public abstract void input();
    public abstract void update(double deltaTime);
    public abstract void render();
    public abstract void shutdown();

    // --- Helper Methods ---
    
    public IGameWindow getWindow() {
        return window;
    }

    public IRenderer getRenderer() {
        return renderer;
    }

    /**
     * Helper to create solid blocks (Walls, Floors, Steps).
     * Uses standard cube and adjusts scale and position.
     */
    protected GameObject createBlock(double x, double y, double z, double sX, double sY, double sZ, Color color) {
        // Creates base cube
        Mesh m = Mesh.createCube();

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
        return obj;
    }

    /**
     * Helper to create inclined ramps (Wedges).
     */
    protected GameObject createRamp(double x, double y, double z, double sX, double sY, double sZ, Color color) {
        Mesh m = Mesh.createWedge();

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
        return obj;
    }

    protected boolean isKeyHeld(int keyCode) {
        return window.isKeyDown(input, keyCode);
    }

    protected boolean isKeyPressed(int keyCode) {
        return window.isKeyPressedOnce(input, keyCode);
    }

    protected boolean isSprintActive() {
        return (window.isKeyDown(Toolkit.getDefaultToolkit(), KeyEvent.VK_CAPS_LOCK));
    }
}