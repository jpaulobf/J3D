package j3d.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import javax.swing.JFrame;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class LwjglWindow implements IGameWindow {

    private long windowHandle;
    private int width;
    private int height;

    public LwjglWindow(String title, int width, int height) {
        this.width = width;
        this.height = height;

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
            glfwSetWindowPos(windowHandle, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(windowHandle);
    }

    @Override
    public void update(int[] pixelBuffer) {
        // In OpenGL, we don't copy a CPU pixel buffer to screen.
        // We just swap the GPU buffers.
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    @Override
    public void destroy() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
    
    public long getHandle() {
        return windowHandle;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    @Override
    public JFrame getFrame() {
        return null;
    }
}