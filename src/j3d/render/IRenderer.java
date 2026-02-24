package j3d.render;

import java.util.List;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

public interface IRenderer {
    void init();
    void clear();
    /** Método genérico de desenho que não conhece a lógica do cenário */
    void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe);
    int[] getFrameBuffer();
}