package j3d.core;

import java.awt.Color;
import java.util.List;
import j3d.geometry.Mesh;
import j3d.geometry.Triangle;
import j3d.geometry.Vertex;
import j3d.graphics.Texture;
import java.util.ArrayList;
import j3d.lighting.PointLight;
import j3d.math.Matrix4;
import j3d.math.Transform;

/**
 * GameObject class representing a game object with a mesh and a transform.
 */
public class GameObject {

    // Mesh representing the geometry of the game object and its transform
    public Mesh mesh;
    public Transform transform = new Transform();
    public static boolean gouraud = true;
    public static boolean scanline = false;

    // Collision Properties
    public boolean hasCollision = true;
    public boolean isVisible = true;
    public boolean isMeshCollision = false; // if true, ignores the horizontal collision of AABB
    public double minX = 0, maxX = 0, minZ = 0, maxZ = 0;
    public double minY = 0, maxY = 0;

    // Culling Properties (Optimization)
    private double cX, cY, cZ, radius;

    // --- Object Pooling / Cache (Garbage Collection Optimization) ---
    // Reusable vertices to avoid allocations inside the render loop
    private final Vertex vCache1 = new Vertex(0, 0, 0);
    private final Vertex vCache2 = new Vertex(0, 0, 0);
    private final Vertex vCache3 = new Vertex(0, 0, 0);
    private final Vertex pCache1 = new Vertex(0, 0, 0);
    private final Vertex pCache2 = new Vertex(0, 0, 0);
    private final Vertex pCache3 = new Vertex(0, 0, 0);
    private final Vertex sCache0 = new Vertex(0, 0, 0);
    private final Vertex sCache1 = new Vertex(0, 0, 0);
    private final Vertex sCache2 = new Vertex(0, 0, 0);
    private final Vertex[] rasterVertices = new Vertex[3];

    /**
     * Constructor for GameObject.
     * 
     * @param m The mesh to be used by this object.
     */
    public GameObject(Mesh m) {
        mesh = m;
        minX = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
        minZ = Double.MAX_VALUE;
        maxZ = -Double.MAX_VALUE;

        // Calculates the collision box limits (AABB) of the object
        for (Vertex v : m.vertices) {
            if (v.x < minX)
                minX = v.x;
            if (v.x > maxX)
                maxX = v.x;
            if (v.y < minY)
                minY = v.y;
            if (v.y > maxY)
                maxY = v.y;
            if (v.z < minZ)
                minZ = v.z;
            if (v.z > maxZ)
                maxZ = v.z;
        }

        // Calculates center and radius of the bounding sphere for fast Culling
        cX = (minX + maxX) / 2.0;
        cY = (minY + maxY) / 2.0;
        cZ = (minZ + maxZ) / 2.0;
        radius = Math.sqrt(Math.pow(maxX - cX, 2) + Math.pow(maxY - cY, 2) + Math.pow(maxZ - cZ, 2));
    }

    /**
     * Get the world height at a given position.
     * 
     * @param worldX
     * @param worldZ
     * @return
     */
    public double getWorldHeightAt(double worldX, double worldZ) {
        // 1. Converts world position to object local space
        double localX = (worldX - transform.x) / transform.scaleX;
        double localZ = (worldZ - transform.z) / transform.scaleZ;

        // 2. Fast check with margin (Epsilon)
        // Increased margin to 0.1 to ensure the player's feet find the triangle
        double eps = 0.1;
        if (localX < minX - eps || localX > maxX + eps || localZ < minZ - eps || localZ > maxZ + eps) {
            return -Double.MAX_VALUE;
        }

        double highestY = -Double.MAX_VALUE;
        boolean hit = false;

        // 3. Narrow phase: Test against each triangle of the mesh
        for (Triangle t : mesh.triangles) {
            Vertex v1 = mesh.vertices.get(t.v1);
            Vertex v2 = mesh.vertices.get(t.v2);
            Vertex v3 = mesh.vertices.get(t.v3);

            // Test if the point (localX, localZ) is inside the triangle projected on the XZ
            // plane
            Double y = getTriangleY(localX, localZ, v1, v2, v3);
            if (y != null) {
                // If the triangle is flat (like the top of a step),
                // we ensure it has priority over internal faces.
                highestY = Math.max(highestY, y);
                hit = true;
            }
        }

        // If there is no direct hit, but we are inside the AABB, we return the top of
        // the AABB
        // only if the object is a simple block, to avoid falls.
        if (!hit)
            return -Double.MAX_VALUE;

        // 4. Converts local height back to world space
        return transform.y + (highestY * transform.scaleY);
    }

    /**
     * Calculates the Y of a point inside a triangle using barycentric coordinates.
     */
    private Double getTriangleY(double px, double pz, Vertex a, Vertex b, Vertex c) {
        double det = (b.z - c.z) * (a.x - c.x) + (c.x - b.x) * (a.z - c.z);
        if (Math.abs(det) < 1e-12)
            return null; // Vertical or degenerate triangle

        double w1 = ((b.z - c.z) * (px - c.x) + (c.x - b.x) * (pz - c.z)) / det;
        double w2 = ((c.z - a.z) * (px - c.x) + (a.x - c.x) * (pz - c.z)) / det;
        double w3 = 1.0 - w1 - w2;

        // Slack to cover micro-gaps between triangles and step edges
        double slack = -0.05;
        if (w1 >= slack && w2 >= slack && w3 >= slack) {
            return w1 * a.y + w2 * b.y + w3 * c.y;
        }
        return null;
    }

    /**
     * Draw method to render the game object using the provided pixels, z-buffer,
     * camera, scene lights, and dimensions.
     * 
     * @param pixels      The pixel array buffer.
     * @param zBuf        The z-buffer for depth testing.
     * @param cam         The active camera.
     * @param sceneLights The list of lights in the scene.
     * @param w           Screen width.
     * @param h           Screen height.
     * @param wire        If true, renders wireframe.
     */
    public void draw(int[] pixels, double[] zBuf, Camera cam, List<PointLight> sceneLights, int w, int h,
            boolean wire) {
        Matrix4 view = cam.getViewMatrix();
        Matrix4 model = transform.getModelMatrix();
        Matrix4 proj = Matrix4.projection(90, (double) w / h, 0.1, 1000);
        Matrix4 modelView = Matrix4.multiply(view, model);

        // --- FRUSTUM CULLING (Optimization) ---
        // Transforms object center to View Space
        Vertex center = new Vertex(cX, cY, cZ);
        Vertex viewCenter = Matrix4.multiply(modelView, center);

        // Adjusts radius based on object scale
        double maxScale = Math.max(transform.scaleX, Math.max(transform.scaleY, transform.scaleZ));
        double r = radius * maxScale;

        // Checks if the sphere is completely behind the camera (Z > -Near) or too far
        // (Z < -Far)
        // Note: In View Space, camera looks at -Z. Objects in front have negative Z.
        if (viewCenter.z - r > -0.1 || viewCenter.z + r < -1000) {
            return; // Invisible object, draw nothing!
        }

        // Pre-calculate lights in View Space
        // Avoids matrix multiplication (heavy operation) for every vertex of every
        // triangle
        java.util.ArrayList<Vertex> viewSpaceLights = new java.util.ArrayList<>();
        if (sceneLights != null) {
            for (PointLight light : sceneLights) {
                viewSpaceLights.add(Matrix4.multiply(view, light.pos));
            }
        }

        for (Triangle t : mesh.triangles) {

            // Optimized: Use cache instead of creating new vertices
            modelView.multiply(mesh.vertices.get(t.v1), vCache1);
            modelView.multiply(mesh.vertices.get(t.v2), vCache2);
            modelView.multiply(mesh.vertices.get(t.v3), vCache3);

            // Aliases for readability (pointing to cache)
            Vertex v1 = vCache1;
            Vertex v2 = vCache2;
            Vertex v3 = vCache3;

            double nx = (v2.y - v1.y) * (v3.z - v1.z) - (v2.z - v1.z) * (v3.y - v1.y);
            double ny = (v2.z - v1.z) * (v3.x - v1.x) - (v2.x - v1.x) * (v3.z - v1.z);
            double nz = (v2.x - v1.x) * (v3.y - v1.y) - (v2.y - v1.y) * (v3.x - v1.x);

            // Backface Culling before normalization
            // Dot product sign doesn't change with normalization, so check before
            if (nx * v1.x + ny * v1.y + nz * v1.z < 0) {

                // Now we normalize, as we need unit vector for lighting
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }

                int c1 = t.baseColor.getRGB(), c2 = t.baseColor.getRGB(), c3 = t.baseColor.getRGB();
                if (sceneLights != null && !wire) {
                    if (gouraud) {
                        c1 = calcLighting(v1, nx, ny, nz, sceneLights, viewSpaceLights, t.baseColor);
                        c2 = calcLighting(v2, nx, ny, nz, sceneLights, viewSpaceLights, t.baseColor);
                        c3 = calcLighting(v3, nx, ny, nz, sceneLights, viewSpaceLights, t.baseColor);
                    } else {
                        double mx = (v1.x + v2.x + v3.x) / 3.0, my = (v1.y + v2.y + v3.y) / 3.0,
                                mz = (v1.z + v2.z + v3.z) / 3.0;
                        int flat = calcLighting(new Vertex(mx, my, mz), nx, ny, nz, sceneLights, viewSpaceLights,
                                t.baseColor);
                        c1 = c2 = c3 = flat;
                    }
                }

                // 1. Projection to Clip Space (not divided by W yet)
                proj.multiply(v1, pCache1);
                proj.multiply(v2, pCache2);
                proj.multiply(v3, pCache3);

                Vertex p1 = pCache1;
                Vertex p2 = pCache2;
                Vertex p3 = pCache3;

                // Pass through original texture coordinates
                p1.u = mesh.vertices.get(t.v1).u;
                p1.v = mesh.vertices.get(t.v1).v;
                p2.u = mesh.vertices.get(t.v2).u;
                p2.v = mesh.vertices.get(t.v2).v;
                p3.u = mesh.vertices.get(t.v3).u;
                p3.v = mesh.vertices.get(t.v3).v;

                // 2. Assemble initial polygon
                List<ClippedVertex> polygon = new ArrayList<>();
                polygon.add(new ClippedVertex(p1, c1));
                polygon.add(new ClippedVertex(p2, c2));
                polygon.add(new ClippedVertex(p3, c3));

                // 3. Apply Clipping (Sutherland-Hodgman on Near Plane)
                polygon = clipPolygon(polygon);

                // 4. Triangulation (Triangle Fan) and Rasterization
                // If clipping generated a Quad (4 vertices), this will draw 2 triangles.
                for (int i = 1; i < polygon.size() - 1; i++) {
                    ClippedVertex cp0 = polygon.get(0);
                    ClippedVertex cp1 = polygon.get(i);
                    ClippedVertex cp2 = polygon.get(i + 1);

                    // Perspective and Viewport (Screen Space)
                    toScreen(cp0.v, w, h, sCache0);
                    toScreen(cp1.v, w, h, sCache1);
                    toScreen(cp2.v, w, h, sCache2);

                    // Reuse array for rasterizer
                    rasterVertices[0] = sCache0;
                    rasterVertices[1] = sCache1;
                    rasterVertices[2] = sCache2;

                    if (wire) {
                        drawWireframe(pixels, rasterVertices, t.baseColor.getRGB(), w, h);
                    } else {
                        if (scanline) {
                            rasterizeScanline(pixels, zBuf, rasterVertices, cp0.color, cp1.color,
                                    cp2.color, t.texture, w, h);
                        } else {
                            rasterize(pixels, zBuf, rasterVertices, cp0.color, cp1.color, cp2.color,
                                    t.texture, w, h);
                        }
                    }
                }
            }
        }
    }

    // Helper class to keep Vertex and Color together during clipping
    private static class ClippedVertex {
        Vertex v;
        int color;

        ClippedVertex(Vertex v, int color) {
            this.v = v;
            this.color = color;
        }
    }

    /**
     * Implementation of Sutherland-Hodgman algorithm (1974) for polygon clipping.
     * Clips geometry against the Near Plane (W = 0.1).
     */
    private List<ClippedVertex> clipPolygon(List<ClippedVertex> vertices) {
        List<ClippedVertex> output = new ArrayList<>();
        double wMin = 0.1; // Near Plane threshold

        for (int i = 0; i < vertices.size(); i++) {
            ClippedVertex current = vertices.get(i);
            ClippedVertex next = vertices.get((i + 1) % vertices.size());

            boolean insideCurrent = current.v.w > wMin;
            boolean insideNext = next.v.w > wMin;

            if (insideCurrent && insideNext) {
                output.add(next);
            } else if (insideCurrent && !insideNext) {
                output.add(intersect(current, next, wMin));
            } else if (!insideCurrent && insideNext) {
                output.add(intersect(current, next, wMin));
                output.add(next);
            }
        }
        return output;
    }

    // Calculates intersection and interpolates color
    private ClippedVertex intersect(ClippedVertex v1, ClippedVertex v2, double wPlane) {
        double t = (wPlane - v1.v.w) / (v2.v.w - v1.v.w);

        // Linear Interpolation of Vertex (X, Y, Z, W)
        Vertex nv = new Vertex(
                v1.v.x + (v2.v.x - v1.v.x) * t,
                v1.v.y + (v2.v.y - v1.v.y) * t,
                v1.v.z + (v2.v.z - v1.v.z) * t);
        nv.w = v1.v.w + (v2.v.w - v1.v.w) * t; // Important to interpolate W as well
        nv.u = v1.v.u + (v2.v.u - v1.v.u) * t;
        nv.v = v1.v.v + (v2.v.v - v1.v.v) * t;

        // Linear Interpolation of Color (R, G, B)
        int c1 = v1.color;
        int c2 = v2.color;
        int r = (int) (((c1 >> 16) & 0xFF) + t * (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)));
        int g = (int) (((c1 >> 8) & 0xFF) + t * (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)));
        int b = (int) ((c1 & 0xFF) + t * ((c2 & 0xFF) - (c1 & 0xFF)));

        // Clamp to avoid color overflow
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new ClippedVertex(nv, (r << 16) | (g << 8) | b);
    }

    // Converts from Clip Space to Screen Space
    // Optimized to write to 'out' vertex instead of creating new
    private void toScreen(Vertex v, int w, int h, Vertex out) {
        // Perspective Divide
        double invW = 1.0 / v.w;
        out.x = (v.x * invW + 1) * w * 0.5;
        out.y = (1 - v.y * invW) * h * 0.5;
        out.z = invW; // Store 1/Z (or 1/W) for Z-Buffer

        // Perspective Correction: Pre-divide U and V by W (original W, not invW)
        out.u = v.u * invW;
        out.v = v.v * invW;
    }

    /**
     * Calculate lighting method to compute the color of a vertex based on its
     * normal, the scene lights, and the base color of the triangle.
     * <p>
     * Based on diffuse reflection model (Lambert) and Gouraud shading (1971).
     * 
     * @param v               The vertex position.
     * @param nx              Normal X.
     * @param ny              Normal Y.
     * @param nz              Normal Z.
     * @param lights          List of lights.
     * @param viewSpaceLights List of lights transformed to view space.
     * @param base            Base color.
     * @return The calculated RGB color.
     */
    private int calcLighting(Vertex v, double nx, double ny, double nz, List<PointLight> lights,
            List<Vertex> viewSpaceLights, Color base) {
        double rT = 0, gT = 0, bT = 0, amb = 0.15;

        // Normalizes object base color (0.0 to 1.0)
        double rb = base.getRed() / 255.0;
        double gb = base.getGreen() / 255.0;
        double bb = base.getBlue() / 255.0;

        for (int i = 0; i < lights.size(); i++) {
            PointLight light = lights.get(i);
            Vertex lV = viewSpaceLights.get(i); // Uses pre-calculated position
            double lx = lV.x - v.x, ly = lV.y - v.y, lz = lV.z - v.z;
            double d = Math.sqrt(lx * lx + ly * ly + lz * lz);
            double dot = Math.max(0, nx * (lx / d) + ny * (ly / d) + nz * (lz / d));
            double att = 1.0 / (1.0 + 0.01 * d * d);

            // Gets light color
            double lr = light.color.getRed() / 255.0;
            double lg = light.color.getGreen() / 255.0;
            double lb = light.color.getBlue() / 255.0;

            // Calculates diffuse contribution considering light and object color
            rT += rb * lr * dot * light.intensity * att;
            gT += gb * lg * dot * light.intensity * att;
            bT += bb * lb * dot * light.intensity * att;
        }

        // Applies ambient light multiplied by base color (avoids gray shadows)
        rT += rb * amb;
        gT += gb * amb;
        bT += bb * amb;

        return new Color((int) (Math.min(1, rT) * 255), (int) (Math.min(1, gT) * 255),
                (int) (Math.min(1, bT) * 255)).getRGB();
    }

    /**
     * Rasterize method to fill the triangle defined by vertices v with the
     * specified color, using the z-buffer for depth testing.
     * <p>
     * Uses Barycentric Coordinates approach (Möbius, 1827) for interpolation.
     * 
     * @param pixels The pixel buffer.
     * @param zBuf   The depth buffer.
     * @param v      Array of 3 vertices (screen space).
     * @param c1     Color of vertex 1.
     * @param c2     Color of vertex 2.
     * @param c3     Color of vertex 3.
     * @param w      Screen width.
     * @param h      Screen height.
     */
    void rasterize(int[] pixels, double[] zBuf, Vertex[] v, int c1, int c2, int c3, Texture tex, int w, int h) {
        int minX = (int) Math.max(0, Math.min(v[0].x, Math.min(v[1].x, v[2].x)));
        int maxX = (int) Math.min(w - 1, Math.max(v[0].x, Math.max(v[1].x, v[2].x)));
        int minY = (int) Math.max(0, Math.min(v[0].y, Math.min(v[1].y, v[2].y)));
        int maxY = (int) Math.min(h - 1, Math.max(v[0].y, Math.max(v[1].y, v[2].y)));
        double area = (v[1].y - v[2].y) * (v[0].x - v[2].x) + (v[2].x - v[1].x) * (v[0].y - v[2].y);
        if (area == 0)
            return;
        double invArea = 1.0 / area; // Optimization 3: Multiplication is much faster than division

        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r3 = (c3 >> 16) & 0xFF, g3 = (c3 >> 8) & 0xFF, b3 = c3 & 0xFF;

        // Pre-calculation of incremental steps (partial derivatives)
        // How much w0, w1 and w2 change when moving 1 pixel in X or Y
        double dw0dx = (v[1].y - v[2].y) * invArea;
        double dw0dy = (v[2].x - v[1].x) * invArea;
        double dw1dx = (v[2].y - v[0].y) * invArea;
        double dw1dy = (v[0].x - v[2].x) * invArea;
        double dw2dx = (v[0].y - v[1].y) * invArea;
        double dw2dy = (v[1].x - v[0].x) * invArea;

        // Initial values of w0, w1, w2 at the top-left corner of the Bounding Box
        // (minX, minY)
        double w0Row = ((v[1].y - v[2].y) * (minX - v[2].x) + (v[2].x - v[1].x) * (minY - v[2].y)) * invArea;
        double w1Row = ((v[2].y - v[0].y) * (minX - v[2].x) + (v[0].x - v[2].x) * (minY - v[2].y)) * invArea;
        double w2Row = ((v[0].y - v[1].y) * (minX - v[0].x) + (v[1].x - v[0].x) * (minY - v[0].y)) * invArea;

        // Pre-calculation of Depth (1/Z) and Color derivatives
        // This allows interpolating Z, R, G, and B using only additions
        double dDepthDx = dw0dx * v[0].z + dw1dx * v[1].z + dw2dx * v[2].z;
        double dDepthDy = dw0dy * v[0].z + dw1dy * v[1].z + dw2dy * v[2].z;

        // UV derivatives (already divided by W in toScreen)
        double dUDx = dw0dx * v[0].u + dw1dx * v[1].u + dw2dx * v[2].u;
        double dUDy = dw0dy * v[0].u + dw1dy * v[1].u + dw2dy * v[2].u;
        double dVDx = dw0dx * v[0].v + dw1dx * v[1].v + dw2dx * v[2].v;
        double dVDy = dw0dy * v[0].v + dw1dy * v[1].v + dw2dy * v[2].v;

        double dRDx = dw0dx * r1 + dw1dx * r2 + dw2dx * r3;
        double dRDy = dw0dy * r1 + dw1dy * r2 + dw2dy * r3;
        double dGDx = dw0dx * g1 + dw1dx * g2 + dw2dx * g3;
        double dGDy = dw0dy * g1 + dw1dy * g2 + dw2dy * g3;
        double dBDx = dw0dx * b1 + dw1dx * b2 + dw2dx * b3;
        double dBDy = dw0dy * b1 + dw1dy * b2 + dw2dy * b3;

        // Initial values at top-left corner
        double depthRow = w0Row * v[0].z + w1Row * v[1].z + w2Row * v[2].z;
        double uRow = w0Row * v[0].u + w1Row * v[1].u + w2Row * v[2].u;
        double vRow = w0Row * v[0].v + w1Row * v[1].v + w2Row * v[2].v;
        double rRow = w0Row * r1 + w1Row * r2 + w2Row * r3;
        double gRow = w0Row * g1 + w1Row * g2 + w2Row * g3;
        double bRow = w0Row * b1 + w1Row * b2 + w2Row * b3;

        for (int y = minY; y <= maxY; y++) {
            // Initializes weights for the start of this line
            double w0 = w0Row;
            double w1 = w1Row;
            double w2 = w2Row;

            double depth = depthRow;
            double u = uRow, vTex = vRow;
            double r = rRow, g = gRow, b = bRow;

            // Optimization: Calculates initial line index outside X loop
            int idx = y * w + minX;

            for (int x = minX; x <= maxX; x++) {
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    // Z-Buffer Optimization: We store 1/Z directly.
                    // Higher value = Closer to camera. No divisions!
                    if (depth > zBuf[idx]) {
                        zBuf[idx] = depth;

                        int finalColor;
                        if (tex != null) {
                            // Recovers real U and V by dividing by 1/W (which is 'depth' here)
                            // Color Multiplication (Modulate)
                            int texColor = tex.getSample(u / depth, vTex / depth);
                            // Mixes texture color with light (Gouraud)
                            // Extracts texture components
                            int tR = (texColor >> 16) & 0xFF;
                            int tG = (texColor >> 8) & 0xFF;
                            int tB = texColor & 0xFF;

                            // Multiplies (r, g, b are 0-255 from light)
                            int fR = (int) (tR * (r / 255.0));
                            int fG = (int) (tG * (g / 255.0));
                            int fB = (int) (tB * (b / 255.0));
                            finalColor = (fR << 16) | (fG << 8) | fB;
                        } else {
                            finalColor = ((int) r << 16) | ((int) g << 8) | (int) b;
                        }
                        pixels[idx] = finalColor;
                    }
                }
                // Increment X: just additions, no multiplications!
                w0 += dw0dx;
                w1 += dw1dx;
                w2 += dw2dx;
                depth += dDepthDx;
                u += dUDx;
                vTex += dVDx;
                r += dRDx;
                g += dGDx;
                b += dBDx;
                idx++; // Advances to next pixel in array linearly
            }
            // Increment Y for next line
            w0Row += dw0dy;
            w1Row += dw1dy;
            w2Row += dw2dy;
            depthRow += dDepthDy;
            uRow += dUDy;
            vRow += dVDy;
            rRow += dRDy;
            gRow += dGDy;
            bRow += dBDy;
        }
    }

    /**
     * Draw wireframe method to draw the edges of the triangle defined by vertices v
     * with the specified color.
     * 
     * @param pixels The pixel buffer.
     * @param v      Array of vertices.
     * @param color  The line color.
     * @param w      Screen width.
     * @param h      Screen height.
     */
    void drawWireframe(int[] pixels, Vertex[] v, int color, int w, int h) {
        drawLine(pixels, v[0], v[1], color, w, h);
        drawLine(pixels, v[1], v[2], color, w, h);
        drawLine(pixels, v[2], v[0], color, w, h);
    }

    /**
     * Draw line method to draw a line between two vertices v1 and v2 with the
     * specified color, using Bresenham's line algorithm.
     * <p>
     * Algorithm developed by Jack Bresenham (1962) at IBM.
     * 
     * @param pixels The pixel buffer.
     * @param v1     Start vertex.
     * @param v2     End vertex.
     * @param color  Line color.
     * @param w      Screen width.
     * @param h      Screen height.
     */
    void drawLine(int[] pixels, Vertex v1, Vertex v2, int color, int w, int h) {
        int x0 = (int) v1.x, y0 = (int) v1.y, x1 = (int) v2.x, y1 = (int) v2.y;
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx - dy;
        while (true) {
            if (x0 >= 0 && x0 < w && y0 >= 0 && y0 < h)
                pixels[y0 * w + x0] = color;
            if (x0 == x1 && y0 == y1)
                break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Alternative rasterization using Scanline algorithm.
     * Useful for performance comparison and study.
     * Classic polygon filling technique (Wylie, Romney, Evans, Erdahl, 1967).
     */
    void rasterizeScanline(int[] pixels, double[] zBuf, Vertex[] v, int c1, int c2, int c3, Texture tex, int w, int h) {
        // 1. Sort vertices by Y (Simple Bubble sort for 3 elements)
        Vertex vMin = v[0], vMid = v[1], vMax = v[2];
        int cMin = c1, cMid = c2, cMax = c3;

        if (vMin.y > vMid.y) {
            Vertex t = vMin;
            vMin = vMid;
            vMid = t;
            int tc = cMin;
            cMin = cMid;
            cMid = tc;
        }
        if (vMin.y > vMax.y) {
            Vertex t = vMin;
            vMin = vMax;
            vMax = t;
            int tc = cMin;
            cMin = cMax;
            cMax = tc;
        }
        if (vMid.y > vMax.y) {
            Vertex t = vMid;
            vMid = vMax;
            vMax = t;
            int tc = cMid;
            cMid = cMax;
            cMax = tc;
        }

        int y1 = (int) vMin.y;
        int y2 = (int) vMid.y;
        int y3 = (int) vMax.y;

        // If triangle has no height or is vertically off-screen, ignore
        if (y1 >= h || y3 < 0 || y1 == y3)
            return;

        // Extract color components
        float r1 = (cMin >> 16) & 0xFF, g1 = (cMin >> 8) & 0xFF, b1 = cMin & 0xFF;
        float r2 = (cMid >> 16) & 0xFF, g2 = (cMid >> 8) & 0xFF, b2 = cMid & 0xFF;
        float r3 = (cMax >> 16) & 0xFF, g3 = (cMax >> 8) & 0xFF, b3 = cMax & 0xFF;

        // OPTIMIZATION: Calculates triangle plane gradients (d/dx) ONCE.
        // This avoids recalculating (zEnd - zStart) / width for every drawn line.
        double den = (vMid.x - vMin.x) * (vMax.y - vMin.y) - (vMax.x - vMin.x) * (vMid.y - vMin.y);
        double invDen = Math.abs(den) < 1e-9 ? 0 : 1.0 / den;

        double dZdx = ((vMid.z - vMin.z) * (vMax.y - vMin.y) - (vMax.z - vMin.z) * (vMid.y - vMin.y)) * invDen;
        double dRdx = ((r2 - r1) * (vMax.y - vMin.y) - (r3 - r1) * (vMid.y - vMin.y)) * invDen;
        double dGdx = ((g2 - g1) * (vMax.y - vMin.y) - (g3 - g1) * (vMid.y - vMin.y)) * invDen;
        double dBdx = ((b2 - b1) * (vMax.y - vMin.y) - (b3 - b1) * (vMid.y - vMin.y)) * invDen;

        // --- Long Edge (vMin -> vMax) ---
        double invHeightLong = 1.0 / (vMax.y - vMin.y);
        double dxLong = (vMax.x - vMin.x) * invHeightLong;
        double dzLong = (vMax.z - vMin.z) * invHeightLong;
        double drLong = (r3 - r1) * invHeightLong;
        double dgLong = (g3 - g1) * invHeightLong;
        double dbLong = (b3 - b1) * invHeightLong;

        double xLong = vMin.x, zLong = vMin.z;
        double rLong = r1, gLong = g1, bLong = b1;

        // --- Top Part (vMin -> vMid) ---
        if (y2 > y1) {
            double invHeight1 = 1.0 / (vMid.y - vMin.y);
            double dx1 = (vMid.x - vMin.x) * invHeight1;
            // double dz1 = (vMid.z - vMin.z) * invHeight1;
            // double dr1 = (r2 - r1) * invHeight1;
            // double dg1 = (g2 - g1) * invHeight1;
            // double db1 = (b2 - b1) * invHeight1;

            double x1_val = vMin.x;// , z1_val = vMin.z;
            // double r1_val = r1, g1_val = g1, b1_val = b1;

            for (int y = y1; y < y2; y++) {
                if (y >= 0 && y < h) {
                    drawScanline(pixels, zBuf, y, w, (int) xLong, (int) x1_val, zLong, rLong, gLong, bLong, dZdx, dRdx,
                            dGdx, dBdx);
                }
                xLong += dxLong;
                zLong += dzLong;
                rLong += drLong;
                gLong += dgLong;
                bLong += dbLong;
                x1_val += dx1;
                // z1_val += dz1;
                // r1_val += dr1;
                // g1_val += dg1;
                // b1_val += db1;
            }
        }

        // --- Bottom Part (vMid -> vMax) ---
        if (y3 > y2) {
            double invHeight2 = 1.0 / (vMax.y - vMid.y);
            double dx2 = (vMax.x - vMid.x) * invHeight2;
            // double dz2 = (vMax.z - vMid.z) * invHeight2;
            // double dr2 = (r3 - r2) * invHeight2;
            // double dg2 = (g3 - g2) * invHeight2;
            // double db2 = (b3 - b2) * invHeight2;

            double x2_val = vMid.x;// , z2_val = vMid.z;
            // double r2_val = r2, g2_val = g2, b2_val = b2;

            for (int y = y2; y < y3; y++) {
                if (y >= 0 && y < h) {
                    drawScanline(pixels, zBuf, y, w, (int) xLong, (int) x2_val, zLong, rLong, gLong, bLong, dZdx, dRdx,
                            dGdx, dBdx);
                }
                xLong += dxLong;
                zLong += dzLong;
                rLong += drLong;
                gLong += dgLong;
                bLong += dbLong;
                x2_val += dx2;
                // z2_val += dz2;
                // r2_val += dr2;
                // g2_val += dg2;
                // b2_val += db2;
            }
        }
    }

    // Draws a horizontal line interpolating Z and Color
    private void drawScanline(int[] pixels, double[] zBuf, int y, int w,
            int xStart, int xEnd,
            double zStart,
            double rStart, double gStart, double bStart,
            double dZdx, double dRdx, double dGdx, double dBdx) {

        // Ensures we draw from left to right
        if (xStart > xEnd) {
            // Attributes (zStart, rStart, etc) correspond to original xStart.
            // If we start drawing at xEnd, we need to calculate attributes at that point.
            double dist = xEnd - xStart; // dist é negativo
            zStart += dist * dZdx;
            rStart += dist * dRdx;
            gStart += dist * dGdx;
            bStart += dist * dBdx;

            // Swap limits
            int temp = xStart;
            xStart = xEnd;
            xEnd = temp;
        }

        if (xEnd < 0 || xStart >= w)
            return;

        int x0 = Math.max(0, xStart);
        int x1 = Math.min(w - 1, xEnd);

        // Adjusts initial values in case line starts off-screen (clipping)
        if (x0 > xStart) {
            double diff = x0 - xStart;
            zStart += dZdx * diff;
            rStart += dRdx * diff;
            gStart += dGdx * diff;
            bStart += dBdx * diff;
        }

        int rowOffset = y * w;
        for (int x = x0; x <= x1; x++) {
            if (zStart > zBuf[rowOffset + x]) {
                zBuf[rowOffset + x] = zStart;
                pixels[rowOffset + x] = ((int) rStart << 16) | ((int) gStart << 8) | (int) bStart;
            }
            zStart += dZdx;
            rStart += dRdx;
            gStart += dGdx;
            bStart += dBdx;
        }
    }
}