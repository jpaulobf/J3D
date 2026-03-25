package j3d.factory;

import j3d.core.IGameWindow;
import j3d.core.LwjglWindow;

/**
 * Create a Window based on the type.
 */
public class WindowFactory {

    /**
     * Create Window based on the type.
     * @param type
     * @param title
     * @param width
     * @param height
     * @return
     */
    public static IGameWindow createWindow(j3d.enums.RenderType type, String title, int width, int height) {
        if (type == j3d.enums.RenderType.OPENGL) {
            IGameWindow window = new j3d.core.LwjglWindow(title, width, height);
            ((LwjglWindow) window).captureCursor();
            return window;
        } else {
            return new j3d.core.Window(title, width, height);
        }
    }
}