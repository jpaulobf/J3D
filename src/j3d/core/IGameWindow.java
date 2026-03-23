package j3d.core;

import java.awt.Point;
import javax.swing.JFrame;

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
}