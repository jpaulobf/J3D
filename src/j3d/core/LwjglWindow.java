package j3d.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import j3d.input.InputManager;
import org.lwjgl.opengl.GL;
import javax.swing.JFrame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.nio.IntBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Window class responsible for creating the game window, managing the canvas
 * for rendering, and displaying the FPS counter.
 */
public class LwjglWindow implements IGameWindow {

    private long windowHandle;
    private int width;
    private int height;
    private boolean isFullscreen = false;
    private int windowedX = 0; 
    private int windowedY = 0; 
    private int windowedW = 0;
    private int windowedH = 0;
    private double lastX = 0;
    private double lastY = 0;
    private boolean firstMouse = true;
    private boolean[] keyState = new boolean[GLFW_KEY_LAST + 1];

    /**
     * Constructor for the Window class, where we initialize the JFrame, set it to
     * fullscreen, and prepare the canvas for rendering.
     * @param title
     * @param width
     * @param height
     */
    public LwjglWindow(String title, int width, int height) {
        this.width = width;
        this.height = height;
        this.windowedW = width;
        this.windowedH = height;

        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create the window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(windowHandle, (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities(); // Inicializa o OpenGL na Thread Principal para permitir chamadas como
                                 // glViewport

        // Detect window resize (Manual or Fullscreen toggle) and update OpenGL Viewport
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h); // Maps the Normalized Device Coordinates to the new screen size
        });

        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(windowHandle);

        captureCursor(); // Lock cursor for FPS style immediately
        toggleFullscreen(); // Start in Fullscreen mode (like Software Renderer)
    }

    /**
     * Updates canvas pixels with renderer data and current FPS, and requests screen
     * repaint.
     * @param pixelBuffer
     */
    @Override
    public void update(int[] pixelBuffer) {
        // In OpenGL, we don't copy a CPU pixel buffer to screen.
        // We just swap the GPU buffers.
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    /**
     * Getter for JFrame, necessary to capture input events and get mouse position.
     * @return
     */
    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    /**
     * Destroyer
     */
    @Override
    public void destroy() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }

    /**
     * Return the Window Handle
     * @return
     */
    public long getHandle() {
        return windowHandle;
    }

    /**
     * Release the Context
     */
    public void releaseContext() {
        glfwMakeContextCurrent(NULL);
    }

    /**
     * Make the Context Current
     */
    public void makeContextCurrent() {
        glfwMakeContextCurrent(windowHandle);
    }

    /**
     * Toggle Fullscreen
     */
    @Override
    public void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode mode = glfwGetVideoMode(monitor);

        if (isFullscreen) {
            // Store current window position and size before switching
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);

                glfwGetWindowPos(windowHandle, x, y);
                glfwGetWindowSize(windowHandle, w, h);

                windowedX = x.get(0);
                windowedY = y.get(0);
                windowedW = w.get(0);
                windowedH = h.get(0);
            }
            // Switch to fullscreen
            glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else {
            // Restore windowed mode
            glfwSetWindowMonitor(windowHandle, NULL, windowedX, windowedY, windowedW, windowedH, 0);
        }
        glfwSwapInterval(1); // Re-enable V-Sync
    }

    //------ Keyboard ------

    /**
     * Verify if a key is pressed
     * @param awtKeyCode
     * @return
     */
    public boolean isKeyDown(int awtKeyCode) {
        int glfwKey = mapAwtToGlfw(awtKeyCode);
        if (glfwKey == -1)
            return false;
        return glfwGetKey(windowHandle, glfwKey) == GLFW_PRESS;
    }

    /**
     * Verify if any key was pressed once
     * @param awtKeyCode
     * @return
     */
    public boolean isKeyPressedOnce(int awtKeyCode) {
        int glfwKey = mapAwtToGlfw(awtKeyCode);
        if (glfwKey == -1)
            return false;

        boolean isDown = glfwGetKey(windowHandle, glfwKey) == GLFW_PRESS;
        boolean wasDown = keyState[glfwKey];
        keyState[glfwKey] = isDown; // Update state

        return isDown && !wasDown; // Rising edge (trigger once)
    }

    /**
     * Map the AWT Keys to GLFW Keys.
     * @param awtCode
     * @return
     */
    private int mapAwtToGlfw(int awtCode) {
        switch (awtCode) {
            case KeyEvent.VK_W:
                return GLFW_KEY_W;
            case KeyEvent.VK_S:
                return GLFW_KEY_S;
            case KeyEvent.VK_A:
                return GLFW_KEY_A;
            case KeyEvent.VK_D:
                return GLFW_KEY_D;
            case KeyEvent.VK_SPACE:
                return GLFW_KEY_SPACE;
            case KeyEvent.VK_ESCAPE:
                return GLFW_KEY_ESCAPE;
            case KeyEvent.VK_F2:
                return GLFW_KEY_F2;
            case KeyEvent.VK_F3:
                return GLFW_KEY_F3;
            case KeyEvent.VK_F4:
                return GLFW_KEY_F4;
            case KeyEvent.VK_F5:
                return GLFW_KEY_F5;
            case KeyEvent.VK_F6:
                return GLFW_KEY_F6;
            case KeyEvent.VK_F10:
                return GLFW_KEY_F10;
            case KeyEvent.VK_F12:
                return GLFW_KEY_F12;
            // Light controls
            case KeyEvent.VK_U:
                return GLFW_KEY_U;
            case KeyEvent.VK_O:
                return GLFW_KEY_O;
            case KeyEvent.VK_J:
                return GLFW_KEY_J;
            case KeyEvent.VK_L:
                return GLFW_KEY_L;
            case KeyEvent.VK_I:
                return GLFW_KEY_I;
            case KeyEvent.VK_K:
                return GLFW_KEY_K;
            // Map CapsLock to Left Shift for Sprinting in GLFW
            case KeyEvent.VK_CAPS_LOCK:
                return GLFW_KEY_LEFT_SHIFT;
            default:
                return -1;
        }
    }

    @Override
    public boolean isKeyDown(InputManager input, int keyCode) {
        return this.isKeyDown(keyCode);
    }

    @Override
    public boolean isKeyPressedOnce(InputManager input, int keyCode) {
        return this.isKeyPressedOnce(keyCode);
    }

    @Override
    public boolean isKeyDown(Toolkit defaultToolkit, int vkCapsLock) {
        try {
            return this.isKeyDown(KeyEvent.VK_CAPS_LOCK);
        } catch (Exception e) {
            return false;
        }
    }

    //------ Mouse -----

    /**
     * Capture the cursor
     */
    public void captureCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    /**
     * Verify if the window is focused
     */
    @Override
    public boolean isFocused() {
        return glfwGetWindowAttrib(windowHandle, GLFW_FOCUSED) == GLFW_TRUE;
    }

    @Override
    public Point getLocationOnScreen() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            glfwGetWindowPos(windowHandle, x, y);
            return new Point(x.get(0), y.get(0));
        }
    }

    /**
     * Get the mouse delta in axis X
     * @param mouseX
     * @param windowCenterX
     * @return
     */
    @Override
    public int getMouseDeltaX(int mouseX, int windowCenterX) {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowHandle, x, y);

        if (firstMouse) {
            lastX = x[0];
            lastY = y[0];
            firstMouse = false;
            return 0;
        }

        int dx = (int) (x[0] - lastX);
        lastX = x[0];
        return dx;
    }

    /**
     * Get the mouse delta in axis Y
     * @param mouseY
     * @param windowCenterY
     * @return
     */
    @Override
    public int getMouseDeltaY(int mouseY, int windowCenterY) {
        double[] y = new double[1];
        glfwGetCursorPos(windowHandle, null, y);

        if (firstMouse) {
            lastY = y[0];
            // Do not set firstMouse = false here, let X handle the flag flip 
            // or strictly handle it if we want independent axes safety.
            return 0;
        }

        int dy = (int) (y[0] - lastY);
        lastY = y[0];
        return dy;
    }

    @Override
    public void requestFocus() {
        glfwFocusWindow(windowHandle);
    }

    @Override
    public void centerMouse() {
        // Do nothing
    }

    @Override
    public void addInputListener(InputManager input) {
        // Do nothing
    }  

    //---------- Getters ------
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public JFrame getFrame() {
        return null;
    }
}