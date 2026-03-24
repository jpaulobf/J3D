package j3d.factory;

import j3d.render.IRenderer;
import j3d.enums.RenderType;

public class RenderFactory {
    public static IRenderer createRenderer(RenderType type, int width, int height) {
        if (RenderType.OPENGL == type) {
            return new j3d.render.OpenGLRenderer(width, height);
        } else {
            return new j3d.render.SoftwareRenderer(width, height);
        }
    }
}
