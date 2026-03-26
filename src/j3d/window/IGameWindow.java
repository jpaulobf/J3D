package j3d.window;

import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.JFrame;
import j3d.input.InputManager;

public interface IGameWindow {
    void update(int[] pixelBuffer);
    boolean shouldClose();
    void destroy();
    int getWidth();
    int getHeight();
    JFrame getFrame();
    void releaseContext();
    void makeContextCurrent();
    boolean isFocused();
    Point getLocationOnScreen();
    void toggleFullscreen();
    int getMouseDeltaX(int mouseX, int windowCenterX);
    int getMouseDeltaY(int mouseY, int windowCenterY);
    boolean isKeyDown(InputManager input, int keyCode);
    boolean isKeyPressedOnce(InputManager input, int keyCode);
    boolean isKeyDown(Toolkit defaultToolkit, int vkCapsLock);
    void centerMouse();
    void addInputListener(InputManager input);
    void requestFocus();
}