package j3d.factory;

import j3d.render.IRenderer;
import j3d.enums.RenderType;

/**
 * Create a Renderer based on the type.
 */
public class RenderFactory {
    /**
     * Create a Renderer based on the type.
     * @param type
     * @param width
     * @param height
     * @return
     */
    public static IRenderer createRenderer(RenderType type, int width, int height) {
        if (RenderType.OPENGL == type) {
            return new j3d.render.OpenGLRenderer(width, height);
        } else {
            return new j3d.render.SoftwareRenderer(width, height);
        }
    }
}
