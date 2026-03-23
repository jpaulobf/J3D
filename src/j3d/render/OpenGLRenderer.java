package j3d.render;

import java.util.List;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.geometry.Triangle;
import j3d.geometry.Vertex;
import j3d.lighting.PointLight;
import java.awt.Color;

/**
 * OpenGLRenderer class implementing the IRenderer interface, responsible for
 * rendering a 3D scene using OpenGL.
 */
public class OpenGLRenderer implements IRenderer {

    private int width = 0;
    private int height = 0;

    /**
     * Constructor for OpenGLRenderer.
     *
     * @param width
     * @param height
     */
    public OpenGLRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Initializes OpenGL capabilities.
     */
    @Override
    public void init() {
        // IMPORTANT: This line allows LWJGL to detect the OpenGL context created by
        // GLFW
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST); // Z-Buffer
        glEnable(GL_CULL_FACE); // Backface culling
        glCullFace(GL_BACK);

        // Lighting Configuration
        glEnable(GL_LIGHTING);
        glEnable(GL_COLOR_MATERIAL); // Objects use their own color (glColor) for ambient/diffuse
        glShadeModel(GL_SMOOTH); // Gouraud Shading

        // Adjusts perspective correction quality
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    }

    /**
     * Clears the frame buffer and depth buffer.
     */
    @Override
    public void clear() {
        glClearColor(0.529f, 0.808f, 0.922f, 1.0f); // Sky Blue (matching SoftwareRenderer gradient logic)
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Draws a 3D scene using OpenGL.
     *
     * @param cam
     * @param objects
     * @param lights
     * @param wireframe
     */
    @Override
    public void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe) {
        // 1. Setup Projection Matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        double aspect = (double) width / height;

        // Simple gluPerspective equivalent
        double fovY = 90.0;
        double zNear = 0.1;
        double zFar = 1000.0;
        double fH = Math.tan(Math.toRadians(fovY) / 2) * zNear;
        double fW = fH * aspect;
        glFrustum(-fW, fW, -fH, fH, zNear, zFar);

        // 2. Setup View Matrix (Camera)
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Align rotation direction with SoftwareRenderer (Positive Angles)
        glRotated(Math.toDegrees(cam.pitch), 1, 0, 0);
        glRotated(Math.toDegrees(cam.yaw), 0, 1, 0);
        glTranslated(-cam.transform.x, -cam.transform.y, -cam.transform.z);

        // 3. Setup Lights
        if (lights != null && !lights.isEmpty()) {
            for (int i = 0; i < Math.min(lights.size(), 8); i++) {
                PointLight l = lights.get(i);
                int lightId = GL_LIGHT0 + i;
                glEnable(lightId);

                // Position (w=1.0 means position, w=0.0 means direction)
                float[] pos = { (float) l.pos.x, (float) l.pos.y, (float) l.pos.z, 1.0f };
                glLightfv(lightId, GL_POSITION, pos);

                // Intensity/Color
                float r = l.color.getRed() / 255f * (float) l.intensity;
                float g = l.color.getGreen() / 255f * (float) l.intensity;
                float b = l.color.getBlue() / 255f * (float) l.intensity;
                glLightfv(lightId, GL_DIFFUSE, new float[] { r, g, b, 1.0f });
            }
        }

        // 4. Render Objects
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glDisable(GL_LIGHTING); // Wireframes usually don't need lighting
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glEnable(GL_LIGHTING);
        }

        for (GameObject obj : objects) {
            glPushMatrix();
            // Model Matrix: Translate -> Rotate (None in Transform yet) -> Scale
            glTranslated(obj.transform.x, obj.transform.y, obj.transform.z);
            glScaled(obj.transform.scaleX, obj.transform.scaleY, obj.transform.scaleZ);

            glBegin(GL_TRIANGLES);
            for (Triangle t : obj.mesh.triangles) {
                // Set Color
                Color c = t.baseColor;
                glColor3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);

                // Send Vertices
                // Note: We should calculate normals per vertex for smooth lighting,
                // but flat normals per triangle is acceptable for now.
                Vertex v1 = obj.mesh.vertices.get(t.v1);
                Vertex v2 = obj.mesh.vertices.get(t.v2);
                Vertex v3 = obj.mesh.vertices.get(t.v3);

                // Calculate simple flat normal
                double nx = (v2.y - v1.y) * (v3.z - v1.z) - (v2.z - v1.z) * (v3.y - v1.y);
                double ny = (v2.z - v1.z) * (v3.x - v1.x) - (v2.x - v1.x) * (v3.z - v1.z);
                double nz = (v2.x - v1.x) * (v3.y - v1.y) - (v2.y - v1.y) * (v3.x - v1.x);
                // Normalize
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }

                glNormal3d(nx, ny, nz);

                // Draw Vertices
                glVertex3d(v1.x, v1.y, v1.z);
                glVertex3d(v2.x, v2.y, v2.z);
                glVertex3d(v3.x, v3.y, v3.z);
            }
            glEnd();
            glPopMatrix();
        }
    }

    /**
     * Draws a 2D sprite using OpenGL.
     *
     * @param spritePixels
     * @param spriteW
     * @param spriteH
     * @param x
     * @param y
     */
    @Override
    public void drawSprite(int[] spritePixels, int spriteW, int spriteH, int x, int y) {
        // Drawing 2D sprites in OpenGL usually requires textures and Ortho projection.
        // For now, we will leave this empty as we are focusing on 3D geometry first.
        // The HUD will disappear temporarily until we implement a Text/Texture
        // renderer.
    }

    /**
     * return the framebuffer
     */
    @Override
    public int[] getFrameBuffer() {
        // OpenGL renders directly to the GPU buffer.
        // We return null because we don't need to pass an int[] to the AWT Window
        // anymore.
        // NOTE: This will break the current Game.java loop until we switch the Window
        // system.
        return new int[0];
    }

    /**
     * ssaa (disable for now)
     */
    @Override
    public boolean isSsaaEnabled() {
        // Hardware MSAA (Multisample Anti-Aliasing) is handled by the window context
        // setup,
        // not manually by the renderer like in SoftwareRenderer.
        return false;
    }
}
