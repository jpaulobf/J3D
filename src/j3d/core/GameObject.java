package j3d.core;

import java.awt.Color;
import java.util.List;

import j3d.geometry.Mesh;
import j3d.geometry.Triangle;
import j3d.lighting.PointLight;
import j3d.math.Matrix4;
import j3d.math.Transform;
import j3d.math.Vertex;

/**
 * GameObject class representing a game object with a mesh and a transform.
 */
public class GameObject {
    
    // Mesh representing the geometry of the game object and its transform
    public Mesh mesh;
    public Transform transform = new Transform();

    /**
     * Constructor for GameObject.
     * @param m
     */
    public GameObject(Mesh m) {
        mesh = m;
    }

    /**
     * Draw method to render the game object using the provided pixels, z-buffer, camera, scene lights, and dimensions.
     * @param pixels
     * @param zBuf
     * @param cam
     * @param sceneLights
     * @param w
     * @param h
     * @param wire
     */
    public void draw(int[] pixels, double[] zBuf, Camera cam, List<PointLight> sceneLights, int w, int h, boolean wire) {
        Matrix4 view = cam.getViewMatrix();
        Matrix4 model = transform.getModelMatrix();
        Matrix4 proj = Matrix4.projection(90, (double) w / h, 0.1, 1000);
        Matrix4 modelView = Matrix4.multiply(view, model);

        for (Triangle t : mesh.triangles) {

            Vertex v1 = Matrix4.multiply(modelView, mesh.vertices.get(t.v1));
            Vertex v2 = Matrix4.multiply(modelView, mesh.vertices.get(t.v2));
            Vertex v3 = Matrix4.multiply(modelView, mesh.vertices.get(t.v3));

            double nx = (v2.y - v1.y) * (v3.z - v1.z) - (v2.z - v1.z) * (v3.y - v1.y);
            double ny = (v2.z - v1.z) * (v3.x - v1.x) - (v2.x - v1.x) * (v3.z - v1.z);
            double nz = (v2.x - v1.x) * (v3.y - v1.y) - (v2.y - v1.y) * (v3.x - v1.x);
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) {
                nx /= len;
                ny /= len;
                nz /= len;
            }

            if (nx * v1.x + ny * v1.y + nz * v1.z < 0) {
                int finalCol = t.baseColor.getRGB();
                if (sceneLights != null && !wire) {
                    double rT = 0, gT = 0, bT = 0, amb = 0.15;
                    double mx = (v1.x + v2.x + v3.x) / 3.0, my = (v1.y + v2.y + v3.y) / 3.0,
                            mz = (v1.z + v2.z + v3.z) / 3.0;
                    for (PointLight light : sceneLights) {
                        Vertex lV = Matrix4.multiply(view, light.pos);
                        double lx = lV.x - mx, ly = lV.y - my, lz = lV.z - mz;
                        double d = Math.sqrt(lx * lx + ly * ly + lz * lz);
                        double dot = Math.max(0, nx * (lx / d) + ny * (ly / d) + nz * (lz / d));
                        double att = 1.0 / (1.0 + 0.01 * d * d);
                        rT += (t.baseColor.getRed() / 255.0) * dot * light.intensity * att;
                        gT += (t.baseColor.getGreen() / 255.0) * dot * light.intensity * att;
                        bT += (t.baseColor.getBlue() / 255.0) * dot * light.intensity * att;
                    }
                    finalCol = new Color((int) (Math.min(1, rT + amb) * 255), (int) (Math.min(1, gT + amb) * 255),
                            (int) (Math.min(1, bT + amb) * 255)).getRGB();
                }

                Vertex[] p = new Vertex[3];
                Vertex[] orig = { v1, v2, v3 };
                boolean clip = false;
                for (int i = 0; i < 3; i++) {
                    Vertex pr = Matrix4.multiply(proj, orig[i]);
                    if (pr.w <= 0.1) {
                        clip = true;
                        break;
                    }
                    p[i] = new Vertex((pr.x / pr.w + 1) * w / 2, (1 - pr.y / pr.w) * h / 2, 1.0 / pr.w);
                }
                if (!clip) {
                    if (wire)
                        drawWireframe(pixels, p, t.baseColor.getRGB(), w, h);
                    else
                        rasterize(pixels, zBuf, p, finalCol, w, h);
                }
            }
        }
    }

    /**
     * Rasterize method to fill the triangle defined by vertices v with the specified color, using the z-buffer for depth testing.
     * @param pixels
     * @param zBuf
     * @param v
     * @param color
     * @param w
     * @param h
     */
    void rasterize(int[] pixels, double[] zBuf, Vertex[] v, int color, int w, int h) {
        int minX = (int) Math.max(0, Math.min(v[0].x, Math.min(v[1].x, v[2].x)));
        int maxX = (int) Math.min(w - 1, Math.max(v[0].x, Math.max(v[1].x, v[2].x)));
        int minY = (int) Math.max(0, Math.min(v[0].y, Math.min(v[1].y, v[2].y)));
        int maxY = (int) Math.min(h - 1, Math.max(v[0].y, Math.max(v[1].y, v[2].y)));
        double area = (v[1].y - v[2].y) * (v[0].x - v[2].x) + (v[2].x - v[1].x) * (v[0].y - v[2].y);
        if (area == 0)
            return;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double w0 = ((v[1].y - v[2].y) * (x - v[2].x) + (v[2].x - v[1].x) * (y - v[2].y)) / area;
                double w1 = ((v[2].y - v[0].y) * (x - v[2].x) + (v[0].x - v[2].x) * (y - v[2].y)) / area;
                double w2 = 1.0 - w0 - w1;
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    double d = w0 * v[0].z + w1 * v[1].z + w2 * v[2].z;
                    int idx = y * w + x;
                    if (d > (1.0 / zBuf[idx])) {
                        zBuf[idx] = 1.0 / d;
                        pixels[idx] = color;
                    }
                }
            }
        }
    }


    /**
     * Draw wireframe method to draw the edges of the triangle defined by vertices v with the specified color.
     * @param pixels
     * @param v
     * @param color
     * @param w
     * @param h
     */
    void drawWireframe(int[] pixels, Vertex[] v, int color, int w, int h) {
        drawLine(pixels, v[0], v[1], color, w, h);
        drawLine(pixels, v[1], v[2], color, w, h);
        drawLine(pixels, v[2], v[0], color, w, h);
    }

    /**
     * Draw line method to draw a line between two vertices v1 and v2 with the specified color, using Bresenham's line algorithm.
     * @param pixels
     * @param v1
     * @param v2
     * @param color
     * @param w
     * @param h
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
}