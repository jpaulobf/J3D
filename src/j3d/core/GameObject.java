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
    public static boolean gouraud = true;

    // Propriedades de Colisão
    public boolean hasCollision = true;
    public double minX = 0, maxX = 0, minZ = 0, maxZ = 0;
    public double minY = 0, maxY = 0;

    /**
     * Constructor for GameObject.
     * 
     * @param m
     */
    public GameObject(Mesh m) {
        mesh = m;
        minX = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
        minZ = Double.MAX_VALUE;
        maxZ = -Double.MAX_VALUE;

        // Calcula os limites da caixa de colisão (AABB) do objeto
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
    }

    /**
     * Draw method to render the game object using the provided pixels, z-buffer,
     * camera, scene lights, and dimensions.
     * 
     * @param pixels
     * @param zBuf
     * @param cam
     * @param sceneLights
     * @param w
     * @param h
     * @param wire
     */
    public void draw(int[] pixels, double[] zBuf, Camera cam, List<PointLight> sceneLights, int w, int h,
            boolean wire) {
        Matrix4 view = cam.getViewMatrix();
        Matrix4 model = transform.getModelMatrix();
        Matrix4 proj = Matrix4.projection(90, (double) w / h, 0.1, 1000);
        Matrix4 modelView = Matrix4.multiply(view, model);

        // Pré-calcular luzes no espaço da câmera (View Space)
        // Evita multiplicar matrizes (operação pesada) para cada vértice de cada
        // triângulo
        java.util.ArrayList<Vertex> viewSpaceLights = new java.util.ArrayList<>();
        if (sceneLights != null) {
            for (PointLight light : sceneLights) {
                viewSpaceLights.add(Matrix4.multiply(view, light.pos));
            }
        }

        for (Triangle t : mesh.triangles) {

            Vertex v1 = Matrix4.multiply(modelView, mesh.vertices.get(t.v1));
            Vertex v2 = Matrix4.multiply(modelView, mesh.vertices.get(t.v2));
            Vertex v3 = Matrix4.multiply(modelView, mesh.vertices.get(t.v3));

            double nx = (v2.y - v1.y) * (v3.z - v1.z) - (v2.z - v1.z) * (v3.y - v1.y);
            double ny = (v2.z - v1.z) * (v3.x - v1.x) - (v2.x - v1.x) * (v3.z - v1.z);
            double nz = (v2.x - v1.x) * (v3.y - v1.y) - (v2.y - v1.y) * (v3.x - v1.x);

            // Backface Culling antes da normalização
            // O sinal do produto escalar não muda com a normalização, então checamos antes
            if (nx * v1.x + ny * v1.y + nz * v1.z < 0) {

                // Agora sim normalizamos, pois precisamos do vetor unitário para a iluminação
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
                        rasterize(pixels, zBuf, p, c1, c2, c3, w, h);
                }
            }
        }
    }

    /**
     * Calculate lighting method to compute the color of a vertex based on its
     * normal, the scene lights, and the base color of the triangle.
     * 
     * @param v
     * @param nx
     * @param ny
     * @param nz
     * @param lights
     * @param viewSpaceLights
     * @param base
     * @return
     */
    private int calcLighting(Vertex v, double nx, double ny, double nz, List<PointLight> lights,
            List<Vertex> viewSpaceLights, Color base) {
        double rT = 0, gT = 0, bT = 0, amb = 0.15;
        for (int i = 0; i < lights.size(); i++) {
            PointLight light = lights.get(i);
            Vertex lV = viewSpaceLights.get(i); // Usa a posição pré-calculada
            double lx = lV.x - v.x, ly = lV.y - v.y, lz = lV.z - v.z;
            double d = Math.sqrt(lx * lx + ly * ly + lz * lz);
            double dot = Math.max(0, nx * (lx / d) + ny * (ly / d) + nz * (lz / d));
            double att = 1.0 / (1.0 + 0.01 * d * d);
            rT += (base.getRed() / 255.0) * dot * light.intensity * att;
            gT += (base.getGreen() / 255.0) * dot * light.intensity * att;
            bT += (base.getBlue() / 255.0) * dot * light.intensity * att;
        }
        return new Color((int) (Math.min(1, rT + amb) * 255), (int) (Math.min(1, gT + amb) * 255),
                (int) (Math.min(1, bT + amb) * 255)).getRGB();
    }

    /**
     * Rasterize method to fill the triangle defined by vertices v with the
     * specified color, using the z-buffer for depth testing.
     * 
     * @param pixels
     * @param zBuf
     * @param v
     * @param c1
     * @param c2
     * @param c3
     * @param w
     * @param h
     */
    void rasterize(int[] pixels, double[] zBuf, Vertex[] v, int c1, int c2, int c3, int w, int h) {
        int minX = (int) Math.max(0, Math.min(v[0].x, Math.min(v[1].x, v[2].x)));
        int maxX = (int) Math.min(w - 1, Math.max(v[0].x, Math.max(v[1].x, v[2].x)));
        int minY = (int) Math.max(0, Math.min(v[0].y, Math.min(v[1].y, v[2].y)));
        int maxY = (int) Math.min(h - 1, Math.max(v[0].y, Math.max(v[1].y, v[2].y)));
        double area = (v[1].y - v[2].y) * (v[0].x - v[2].x) + (v[2].x - v[1].x) * (v[0].y - v[2].y);
        if (area == 0)
            return;
        double invArea = 1.0 / area; // Otimização 3: Multiplicação é muito mais rápida que divisão

        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r3 = (c3 >> 16) & 0xFF, g3 = (c3 >> 8) & 0xFF, b3 = c3 & 0xFF;

        // Pré-cálculo dos passos incrementais (derivadas parciais)
        // Quanto w0, w1 e w2 mudam ao andar 1 pixel em X ou Y
        double dw0dx = (v[1].y - v[2].y) * invArea;
        double dw0dy = (v[2].x - v[1].x) * invArea;
        double dw1dx = (v[2].y - v[0].y) * invArea;
        double dw1dy = (v[0].x - v[2].x) * invArea;
        double dw2dx = (v[0].y - v[1].y) * invArea;
        double dw2dy = (v[1].x - v[0].x) * invArea;

        // Valores iniciais de w0, w1, w2 no canto superior esquerdo do Bounding Box (minX, minY)
        double w0Row = ((v[1].y - v[2].y) * (minX - v[2].x) + (v[2].x - v[1].x) * (minY - v[2].y)) * invArea;
        double w1Row = ((v[2].y - v[0].y) * (minX - v[2].x) + (v[0].x - v[2].x) * (minY - v[2].y)) * invArea;
        double w2Row = ((v[0].y - v[1].y) * (minX - v[0].x) + (v[1].x - v[0].x) * (minY - v[0].y)) * invArea;

        for (int y = minY; y <= maxY; y++) {
            // Inicializa os pesos para o começo desta linha
            double w0 = w0Row;
            double w1 = w1Row;
            double w2 = w2Row;

            for (int x = minX; x <= maxX; x++) {
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    double d = w0 * v[0].z + w1 * v[1].z + w2 * v[2].z;
                    int idx = y * w + x;
                    if (d > (1.0 / zBuf[idx])) {
                        zBuf[idx] = 1.0 / d;
                        int r = (int) (w0 * r1 + w1 * r2 + w2 * r3);
                        int g = (int) (w0 * g1 + w1 * g2 + w2 * g3);
                        int b = (int) (w0 * b1 + w1 * b2 + w2 * b3);
                        pixels[idx] = (r << 16) | (g << 8) | b;
                    }
                }
                // Incrementa X: apenas somas, sem multiplicações!
                w0 += dw0dx;
                w1 += dw1dx;
                w2 += dw2dx;
            }
            // Incrementa Y para a próxima linha
            w0Row += dw0dy;
            w1Row += dw1dy;
            w2Row += dw2dy;
        }
    }

    /**
     * Draw wireframe method to draw the edges of the triangle defined by vertices v
     * with the specified color.
     * 
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
     * Draw line method to draw a line between two vertices v1 and v2 with the
     * specified color, using Bresenham's line algorithm.
     * 
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