package j3d.core;

import j3d.enums.RenderType;
import j3d.factory.RenderFactory;
import j3d.factory.WindowFactory;
import j3d.render.IRenderer;
import java.awt.Point;

public abstract class AbstractGame implements Runnable {

    protected IGameWindow window;
    protected IRenderer renderer;
    protected boolean running = true;
    protected int width;
    protected int height;
    protected int targetFps = 60;
    
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

            boolean shouldRender = false;

            while (delta >= 1) {
                // Fixed Update
                input();
                update(1.0 / targetFps);
                delta--;
                shouldRender = true;
            }

            if (shouldRender) {
                render();
                // Swap Buffers / Copy to Screen
                window.update(renderer.getFrameBuffer());
                framesCount++;
            } else {
                try { Thread.sleep(1); } catch (InterruptedException e) {}
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
}
