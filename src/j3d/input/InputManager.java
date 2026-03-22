package j3d.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * InputManager centralizes keyboard and mouse state.
 */
public class InputManager implements KeyListener, MouseMotionListener, MouseWheelListener {
    private boolean[] keys = new boolean[256];
    private boolean[] justPressed = new boolean[256];

    // We store only the raw mouse position on screen
    private int mouseX, mouseY;
    private int scrollDelta = 0;

    /**
     * Retrieves the scroll delta and resets it.
     * 
     * @return The amount the mouse wheel was rotated.
     */
    public int getScrollDelta() {
        int val = scrollDelta;
        scrollDelta = 0; // Resets after reading to avoid continuous movement
        return val;
    }

    /**
     * @return The current raw X position of the mouse on screen.
     */
    public int getMouseX() {
        return mouseX;
    }

    /**
     * @return The current raw Y position of the mouse on screen.
     */
    public int getMouseY() {
        return mouseY;
    }

    /**
     * Checks if a key is currently being held down.
     * 
     * @param keyCode The key code to check.
     * @return True if the key is pressed, false otherwise.
     */
    public boolean isKeyHeld(int keyCode) {
        return keyCode >= 0 && keyCode < 256 && keys[keyCode];
    }

    /**
     * Checks if a key was just pressed in the current frame (one-shot).
     * This prevents repeated actions when holding a key.
     * 
     * @param keyCode The key code to check.
     * @return True if the key was just pressed.
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < 256 && keys[keyCode] && !justPressed[keyCode]) {
            justPressed[keyCode] = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // We only capture raw position
        mouseX = e.getXOnScreen();
        mouseY = e.getYOnScreen();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256)
            keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256) {
            keys[e.getKeyCode()] = false;
            justPressed[e.getKeyCode()] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scrollDelta += e.getWheelRotation();
    }
}