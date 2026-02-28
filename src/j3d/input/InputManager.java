package j3d.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * InputManager centraliza o estado do teclado e do rato.
 */
public class InputManager implements KeyListener, MouseMotionListener, MouseWheelListener {
    private boolean[] keys = new boolean[256];
    private boolean[] justPressed = new boolean[256];

    // Armazenamos apenas a posição bruta do mouse na tela
    private int mouseX, mouseY;
    private int scrollDelta = 0;

    public int getScrollDelta() {
        int val = scrollDelta;
        scrollDelta = 0; // Reseta após a leitura para não continuar movendo
        return val;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean isKeyHeld(int keyCode) {
        return keyCode >= 0 && keyCode < 256 && keys[keyCode];
    }

    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < 256 && keys[keyCode] && !justPressed[keyCode]) {
            justPressed[keyCode] = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Apenas capturamos a posição bruta
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