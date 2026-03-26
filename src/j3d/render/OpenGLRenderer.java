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
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenGLRenderer class implementing the IRenderer interface, responsible for
 * rendering a 3D scene using OpenGL.
 */
public class OpenGLRenderer implements IRenderer {

    private int width = 0;
    private int height = 0;
    private int hudTextureId = -1; // Cache for the HUD texture ID
    private final Map<j3d.graphics.Texture, Integer> textureCache = new HashMap<>();

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

    public void setWidthHeight(int width, int height) {
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
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        glShadeModel(GL_SMOOTH); // Gouraud Shading
        glEnable(GL_NORMALIZE); // Ensures normals are unit length after scaling

        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE); // Garante que a textura interaja com a luz
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

            j3d.graphics.Texture lastBoundTexture = null;
            glBegin(GL_TRIANGLES);
            for (Triangle t : obj.mesh.triangles) {
                // 1. Optimized Texture Switching
                if (t.texture != lastBoundTexture) {
                    glEnd(); // End current triangle batch to change texture state
                    if (t.texture != null) {
                        glEnable(GL_TEXTURE_2D);
                        glBindTexture(GL_TEXTURE_2D, getOrCreateTexture(t.texture));
                    } else {
                        glDisable(GL_TEXTURE_2D);
                    }
                    lastBoundTexture = t.texture;
                    glBegin(GL_TRIANGLES);
                }

                // 2. Set Color (Modulates with texture if active)
                if (t.texture != null) {
                    // Usamos branco para não criar gradientes indesejados sobre a textura
                    glColor3f(1.0f, 1.0f, 1.0f);
                } else {
                    Color c = t.baseColor;
                    glColor3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
                }

                Vertex v1 = obj.mesh.vertices.get(t.v1);
                Vertex v2 = obj.mesh.vertices.get(t.v2);
                Vertex v3 = obj.mesh.vertices.get(t.v3);

                // 3. Normal Calculation (Flat)
                double nx = (v2.y - v1.y) * (v3.z - v1.z) - (v2.z - v1.z) * (v3.y - v1.y);
                double ny = (v2.z - v1.z) * (v3.x - v1.x) - (v2.x - v1.x) * (v3.z - v1.z);
                double nz = (v2.x - v1.x) * (v3.y - v1.y) - (v2.y - v1.y) * (v3.x - v1.x);

                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                glNormal3d(nx, ny, nz);

                // 4. Draw Vertices with UVs (Inverting V for OpenGL standard)
                glTexCoord2d(v1.u, 1.0 - v1.v);
                glVertex3d(v1.x, v1.y, v1.z);
                glTexCoord2d(v2.u, 1.0 - v2.v);
                glVertex3d(v2.x, v2.y, v2.z);
                glTexCoord2d(v3.u, 1.0 - v3.v);
                glVertex3d(v3.x, v3.y, v3.z);
            }
            glEnd();
            glDisable(GL_TEXTURE_2D); // Reset state for next object
            glPopMatrix();
        }
    }

    /**
     * Helper to load J3D Texture into OpenGL ID with caching.
     */
    private int getOrCreateTexture(j3d.graphics.Texture texture) {
        if (textureCache.containsKey(texture)) {
            return textureCache.get(texture);
        }

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        // Crucial for textures with widths not multiple of 4
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        // J3D Texture uses ARGB, OpenGL expects RGBA
        int[] pixels = texture.getPixels();
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
            buffer.put((byte) (pixel & 0xFF)); // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texture.getWidth(), texture.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE,
                buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        textureCache.put(texture, id);
        return id;
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
        // 1. Prepare Texture Data
        // We assume input is ARGB, we need RGBA for OpenGL
        ByteBuffer buffer = BufferUtils.createByteBuffer(spriteW * spriteH * 4);
        for (int pixel : spritePixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
            buffer.put((byte) (pixel & 0xFF)); // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        // Create texture ID if not exists
        if (hudTextureId == -1) {
            hudTextureId = glGenTextures();
        }

        // 2. Setup GL State for 2D Overlay
        glDisable(GL_DEPTH_TEST); // HUD should draw over everything
        glDisable(GL_CULL_FACE); // Disable culling for 2D
        glDisable(GL_LIGHTING); // Disable lighting for 2D (use texture colors)
        glEnable(GL_BLEND); // Enable Transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);

        // Switch to Orthographic Projection (2D Screen Coordinates)
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1); // Top-left is (0,0)

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glTranslated(x, y, 0); // Position the sprite

        // 3. Upload and Draw Texture
        glBindTexture(GL_TEXTURE_2D, hudTextureId);
        // Nearest filter keeps the pixel-art look of your software renderer
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, spriteW, spriteH, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glColor4f(1, 1, 1, 1); // White tint to draw texture as-is
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(0, 0);
        glTexCoord2f(1, 0);
        glVertex2f(spriteW, 0);
        glTexCoord2f(1, 1);
        glVertex2f(spriteW, spriteH);
        glTexCoord2f(0, 1);
        glVertex2f(0, spriteH);
        glEnd();

        // 4. Restore State
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE); // Restore Culling for 3D
        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
    }

    /**
     * return the framebuffer
     */
    @Override
    public int[] getFrameBuffer() {
        return null;
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

    @Override
    public void toggleSsaa() {
        // do nothing
    }
}
