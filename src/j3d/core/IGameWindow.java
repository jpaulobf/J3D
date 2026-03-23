package j3d.core;

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
}