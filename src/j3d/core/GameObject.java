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

    // Propriedades de Colisão
    public boolean hasCollision = true;
    public double minX = 0, maxX = 0, minZ = 0, maxZ = 0;
    public double minY = 0, maxY = 0;

    // Propriedades para Culling (Otimização)
    private double cX, cY, cZ, radius;

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

        // Calcula o centro e o raio da esfera envolvente para Culling rápido
        cX = (minX + maxX) / 2.0;
        cY = (minY + maxY) / 2.0;
        cZ = (minZ + maxZ) / 2.0;
        radius = Math.sqrt(Math.pow(maxX - cX, 2) + Math.pow(maxY - cY, 2) + Math.pow(maxZ - cZ, 2));
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

        // --- FRUSTUM CULLING (Otimização) ---
        // Transforma o centro do objeto para o espaço da câmera (View Space)
        Vertex center = new Vertex(cX, cY, cZ);
        Vertex viewCenter = Matrix4.multiply(modelView, center);

        // Ajusta o raio com base na escala do objeto
        double maxScale = Math.max(transform.scaleX, Math.max(transform.scaleY, transform.scaleZ));
        double r = radius * maxScale;

        // Verifica se a esfera está totalmente atrás da câmera (Z > -Near) ou muito longe (Z < -Far)
        // Nota: No View Space, a câmera olha para -Z. Objetos na frente têm Z negativo.
        if (viewCenter.z - r > -0.1 || viewCenter.z + r < -1000) {
            return; // Objeto invisível, não desenha nada!
        }

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

                // 1. Projeção para Clip Space (ainda sem dividir por W)
                Vertex p1 = Matrix4.multiply(proj, v1);
                Vertex p2 = Matrix4.multiply(proj, v2);
                Vertex p3 = Matrix4.multiply(proj, v3);
                
                // Repassa coordenadas de textura dos vértices originais
                p1.u = mesh.vertices.get(t.v1).u; p1.v = mesh.vertices.get(t.v1).v;
                p2.u = mesh.vertices.get(t.v2).u; p2.v = mesh.vertices.get(t.v2).v;
                p3.u = mesh.vertices.get(t.v3).u; p3.v = mesh.vertices.get(t.v3).v;

                // 2. Monta o polígono inicial
                List<ClippedVertex> polygon = new ArrayList<>();
                polygon.add(new ClippedVertex(p1, c1));
                polygon.add(new ClippedVertex(p2, c2));
                polygon.add(new ClippedVertex(p3, c3));

                // 3. Aplica Clipping (Sutherland-Hodgman no Near Plane)
                polygon = clipPolygon(polygon);

                // 4. Triangulação (Triangle Fan) e Rasterização
                // Se o clipping gerou um Quad (4 vértices), isso vai desenhar 2 triângulos.
                for (int i = 1; i < polygon.size() - 1; i++) {
                    ClippedVertex cp0 = polygon.get(0);
                    ClippedVertex cp1 = polygon.get(i);
                    ClippedVertex cp2 = polygon.get(i + 1);

                    // Perspectiva e Viewport (Screen Space)
                    Vertex s0 = toScreen(cp0.v, w, h);
                    Vertex s1 = toScreen(cp1.v, w, h);
                    Vertex s2 = toScreen(cp2.v, w, h);

                    if (wire) {
                        drawWireframe(pixels, new Vertex[]{s0, s1, s2}, t.baseColor.getRGB(), w, h);
                    } else {
                        if (scanline) {
                            rasterizeScanline(pixels, zBuf, new Vertex[]{s0, s1, s2}, cp0.color, cp1.color, cp2.color, t.texture, w, h);
                        } else {
                            rasterize(pixels, zBuf, new Vertex[]{s0, s1, s2}, cp0.color, cp1.color, cp2.color, t.texture, w, h);
                        }
                    }
                }
            }
        }
    }

    // Classe auxiliar para manter Vértice e Cor juntos durante o clipping
    private static class ClippedVertex {
        Vertex v;
        int color;
        ClippedVertex(Vertex v, int color) { this.v = v; this.color = color; }
    }

    /**
     * Implementação do algoritmo de Sutherland-Hodgman (1974) para clipping de polígonos.
     * Realiza o corte da geometria contra o Near Plane (W = 0.1).
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

    // Calcula a interseção e interpola a cor
    private ClippedVertex intersect(ClippedVertex v1, ClippedVertex v2, double wPlane) {
        double t = (wPlane - v1.v.w) / (v2.v.w - v1.v.w);

        // Interpolação Linear do Vértice (X, Y, Z, W)
        Vertex nv = new Vertex(
            v1.v.x + (v2.v.x - v1.v.x) * t,
            v1.v.y + (v2.v.y - v1.v.y) * t,
            v1.v.z + (v2.v.z - v1.v.z) * t
        );
        nv.w = v1.v.w + (v2.v.w - v1.v.w) * t; // Importante interpolar W também
        nv.u = v1.v.u + (v2.v.u - v1.v.u) * t;
        nv.v = v1.v.v + (v2.v.v - v1.v.v) * t;

        // Interpolação Linear da Cor (R, G, B)
        int c1 = v1.color;
        int c2 = v2.color;
        int r = (int) (((c1 >> 16) & 0xFF) + t * (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)));
        int g = (int) (((c1 >> 8) & 0xFF) + t * (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)));
        int b = (int) ((c1 & 0xFF) + t * ((c2 & 0xFF) - (c1 & 0xFF)));
        
        // Clamp para evitar overflow de cor
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new ClippedVertex(nv, (r << 16) | (g << 8) | b);
    }

    // Converte de Clip Space para Screen Space
    private Vertex toScreen(Vertex v, int w, int h) {
        // Perspectiva Divide
        double invW = 1.0 / v.w;
        Vertex s = new Vertex(
            (v.x * invW + 1) * w * 0.5, 
            (1 - v.y * invW) * h * 0.5, 
            invW // Armazenamos 1/Z (ou 1/W) para o Z-Buffer
        );
        // Correção de Perspectiva: Pré-divide U e V por W (W original, não invW)
        // Aqui usamos invW para multiplicar, que é o mesmo que dividir por W.
        s.u = v.u * invW;
        s.v = v.v * invW;
        return s;
    }

    /**
     * Calculate lighting method to compute the color of a vertex based on its
     * normal, the scene lights, and the base color of the triangle.
     * <p>
     * Baseado no modelo de reflexão difusa (Lambert) e sombreamento de Gouraud (1971).
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
        
        // Normaliza a cor base do objeto (0.0 a 1.0)
        double rb = base.getRed() / 255.0;
        double gb = base.getGreen() / 255.0;
        double bb = base.getBlue() / 255.0;

        for (int i = 0; i < lights.size(); i++) {
            PointLight light = lights.get(i);
            Vertex lV = viewSpaceLights.get(i); // Usa a posição pré-calculada
            double lx = lV.x - v.x, ly = lV.y - v.y, lz = lV.z - v.z;
            double d = Math.sqrt(lx * lx + ly * ly + lz * lz);
            double dot = Math.max(0, nx * (lx / d) + ny * (ly / d) + nz * (lz / d));
            double att = 1.0 / (1.0 + 0.01 * d * d);
            
            // Obtém a cor da luz
            double lr = light.color.getRed() / 255.0;
            double lg = light.color.getGreen() / 255.0;
            double lb = light.color.getBlue() / 255.0;

            // Calcula a contribuição difusa considerando a cor da luz e do objeto
            rT += rb * lr * dot * light.intensity * att;
            gT += gb * lg * dot * light.intensity * att;
            bT += bb * lb * dot * light.intensity * att;
        }
        
        // Aplica a luz ambiente multiplicada pela cor base (evita sombras cinzas)
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
     * Utiliza a abordagem de Coordenadas Baricêntricas (Möbius, 1827) para interpolação.
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
    void rasterize(int[] pixels, double[] zBuf, Vertex[] v, int c1, int c2, int c3, Texture tex, int w, int h) {
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

        // Pré-cálculo das derivadas de Profundidade (1/Z) e Cor
        // Isso permite interpolar Z, R, G e B usando apenas somas
        double dDepthDx = dw0dx * v[0].z + dw1dx * v[1].z + dw2dx * v[2].z;
        double dDepthDy = dw0dy * v[0].z + dw1dy * v[1].z + dw2dy * v[2].z;

        // Derivadas de UV (já divididos por W em toScreen)
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

        // Valores iniciais no canto superior esquerdo
        double depthRow = w0Row * v[0].z + w1Row * v[1].z + w2Row * v[2].z;
        double uRow = w0Row * v[0].u + w1Row * v[1].u + w2Row * v[2].u;
        double vRow = w0Row * v[0].v + w1Row * v[1].v + w2Row * v[2].v;
        double rRow = w0Row * r1 + w1Row * r2 + w2Row * r3;
        double gRow = w0Row * g1 + w1Row * g2 + w2Row * g3;
        double bRow = w0Row * b1 + w1Row * b2 + w2Row * b3;

        for (int y = minY; y <= maxY; y++) {
            // Inicializa os pesos para o começo desta linha
            double w0 = w0Row;
            double w1 = w1Row;
            double w2 = w2Row;
            
            double depth = depthRow;
            double u = uRow, vTex = vRow;
            double r = rRow, g = gRow, b = bRow;

            // Otimização: Calcula o índice inicial da linha fora do loop X
            int idx = y * w + minX;

            for (int x = minX; x <= maxX; x++) {
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    // Otimização Z-Buffer: Armazenamos 1/Z diretamente.
                    // Maior valor = Mais perto da câmera. Sem divisões!
                    if (depth > zBuf[idx]) {
                        zBuf[idx] = depth;
                        
                        int finalColor;
                        if (tex != null) {
                            // Recupera U e V reais dividindo por 1/W (que é o 'depth' aqui)
                            // Multiplicação por cor (Modulate)
                            int texColor = tex.getSample(u / depth, vTex / depth);
                            // Mistura a cor da textura com a luz (Gouraud)
                            // Extrai componentes da textura
                            int tR = (texColor >> 16) & 0xFF;
                            int tG = (texColor >> 8) & 0xFF;
                            int tB = texColor & 0xFF;
                            
                            // Multiplica (r, g, b são 0-255 da luz)
                            int fR = (int)(tR * (r / 255.0));
                            int fG = (int)(tG * (g / 255.0));
                            int fB = (int)(tB * (b / 255.0));
                            finalColor = (fR << 16) | (fG << 8) | fB;
                        } else {
                            finalColor = ((int)r << 16) | ((int)g << 8) | (int)b;
                        }
                        pixels[idx] = finalColor;
                    }
                }
                // Incrementa X: apenas somas, sem multiplicações!
                w0 += dw0dx;
                w1 += dw1dx;
                w2 += dw2dx;
                depth += dDepthDx;
                u += dUDx; vTex += dVDx;
                r += dRDx; g += dGDx; b += dBDx;
                idx++; // Avança para o próximo pixel no array linearmente
            }
            // Incrementa Y para a próxima linha
            w0Row += dw0dy;
            w1Row += dw1dy;
            w2Row += dw2dy;
            depthRow += dDepthDy;
            uRow += dUDy; vRow += dVDy;
            rRow += dRDy; gRow += dGDy; bRow += dBDy;
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
     * <p>
     * Algoritmo desenvolvido por Jack Bresenham (1962) na IBM.
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

    /**
     * Rasterização alternativa usando algoritmo Scanline (Linha de Varredura).
     * Útil para comparação de performance e estudo.
     * Técnica clássica de preenchimento de polígonos (Wylie, Romney, Evans, Erdahl, 1967).
     */
    void rasterizeScanline(int[] pixels, double[] zBuf, Vertex[] v, int c1, int c2, int c3, Texture tex, int w, int h) {
        // 1. Ordena vértices por Y (Bubble sort simples para 3 elementos)
        Vertex vMin = v[0], vMid = v[1], vMax = v[2];
        int cMin = c1, cMid = c2, cMax = c3;

        if (vMin.y > vMid.y) { Vertex t = vMin; vMin = vMid; vMid = t; int tc = cMin; cMin = cMid; cMid = tc; }
        if (vMin.y > vMax.y) { Vertex t = vMin; vMin = vMax; vMax = t; int tc = cMin; cMin = cMax; cMax = tc; }
        if (vMid.y > vMax.y) { Vertex t = vMid; vMid = vMax; vMax = t; int tc = cMid; cMid = cMax; cMax = tc; }

        int y1 = (int) vMin.y;
        int y2 = (int) vMid.y;
        int y3 = (int) vMax.y;

        // Se o triângulo não tem altura ou está fora da tela verticalmente, ignora
        if (y1 >= h || y3 < 0 || y1 == y3) return;

        // Extrai componentes de cor
        float r1 = (cMin >> 16) & 0xFF, g1 = (cMin >> 8) & 0xFF, b1 = cMin & 0xFF;
        float r2 = (cMid >> 16) & 0xFF, g2 = (cMid >> 8) & 0xFF, b2 = cMid & 0xFF;
        float r3 = (cMax >> 16) & 0xFF, g3 = (cMax >> 8) & 0xFF, b3 = cMax & 0xFF;

        // OTIMIZAÇÃO: Calcula os gradientes (d/dx) do plano do triângulo UMA VEZ.
        // Isso evita recalcular (zEnd - zStart) / width a cada linha desenhada.
        double den = (vMid.x - vMin.x) * (vMax.y - vMin.y) - (vMax.x - vMin.x) * (vMid.y - vMin.y);
        double invDen = Math.abs(den) < 1e-9 ? 0 : 1.0 / den;

        double dZdx = ((vMid.z - vMin.z) * (vMax.y - vMin.y) - (vMax.z - vMin.z) * (vMid.y - vMin.y)) * invDen;
        double dRdx = ((r2 - r1) * (vMax.y - vMin.y) - (r3 - r1) * (vMid.y - vMin.y)) * invDen;
        double dGdx = ((g2 - g1) * (vMax.y - vMin.y) - (g3 - g1) * (vMid.y - vMin.y)) * invDen;
        double dBdx = ((b2 - b1) * (vMax.y - vMin.y) - (b3 - b1) * (vMid.y - vMin.y)) * invDen;

        // --- Aresta Longa (vMin -> vMax) ---
        double invHeightLong = 1.0 / (vMax.y - vMin.y);
        double dxLong = (vMax.x - vMin.x) * invHeightLong;
        double dzLong = (vMax.z - vMin.z) * invHeightLong;
        double drLong = (r3 - r1) * invHeightLong;
        double dgLong = (g3 - g1) * invHeightLong;
        double dbLong = (b3 - b1) * invHeightLong;

        double xLong = vMin.x, zLong = vMin.z;
        double rLong = r1, gLong = g1, bLong = b1;

        // --- Parte Superior (vMin -> vMid) ---
        if (y2 > y1) {
            double invHeight1 = 1.0 / (vMid.y - vMin.y);
            double dx1 = (vMid.x - vMin.x) * invHeight1;
            double dz1 = (vMid.z - vMin.z) * invHeight1;
            double dr1 = (r2 - r1) * invHeight1;
            double dg1 = (g2 - g1) * invHeight1;
            double db1 = (b2 - b1) * invHeight1;

            double x1_val = vMin.x, z1_val = vMin.z;
            double r1_val = r1, g1_val = g1, b1_val = b1;

            for (int y = y1; y < y2; y++) {
                if (y >= 0 && y < h) {
                    drawScanline(pixels, zBuf, y, w, (int)xLong, (int)x1_val, zLong, rLong, gLong, bLong, dZdx, dRdx, dGdx, dBdx);
                }
                xLong += dxLong; zLong += dzLong; rLong += drLong; gLong += dgLong; bLong += dbLong;
                x1_val += dx1; z1_val += dz1; r1_val += dr1; g1_val += dg1; b1_val += db1;
            }
        }

        // --- Parte Inferior (vMid -> vMax) ---
        if (y3 > y2) {
            double invHeight2 = 1.0 / (vMax.y - vMid.y);
            double dx2 = (vMax.x - vMid.x) * invHeight2;
            double dz2 = (vMax.z - vMid.z) * invHeight2;
            double dr2 = (r3 - r2) * invHeight2;
            double dg2 = (g3 - g2) * invHeight2;
            double db2 = (b3 - b2) * invHeight2;

            double x2_val = vMid.x, z2_val = vMid.z;
            double r2_val = r2, g2_val = g2, b2_val = b2;

            for (int y = y2; y < y3; y++) {
                if (y >= 0 && y < h) {
                    drawScanline(pixels, zBuf, y, w, (int)xLong, (int)x2_val, zLong, rLong, gLong, bLong, dZdx, dRdx, dGdx, dBdx);
                }
                xLong += dxLong; zLong += dzLong; rLong += drLong; gLong += dgLong; bLong += dbLong;
                x2_val += dx2; z2_val += dz2; r2_val += dr2; g2_val += dg2; b2_val += db2;
            }
        }
    }

    // Desenha uma linha horizontal interpolando Z e Cor
    private void drawScanline(int[] pixels, double[] zBuf, int y, int w, 
                              int xStart, int xEnd, 
                              double zStart, 
                              double rStart, double gStart, double bStart,
                              double dZdx, double dRdx, double dGdx, double dBdx) {
        
        // Garante que desenhamos da esquerda para a direita
        if (xStart > xEnd) {
            // Os atributos (zStart, rStart, etc) correspondem ao xStart original.
            // Se vamos começar a desenhar em xEnd, precisamos calcular os atributos nesse ponto.
            double dist = xEnd - xStart; // dist é negativo
            zStart += dist * dZdx;
            rStart += dist * dRdx;
            gStart += dist * dGdx;
            bStart += dist * dBdx;
            
            // Troca os limites
            int temp = xStart;
            xStart = xEnd;
            xEnd = temp;
        }

        if (xEnd < 0 || xStart >= w) return;

        int x0 = Math.max(0, xStart);
        int x1 = Math.min(w - 1, xEnd);

        // Ajusta valores iniciais para o caso de a linha começar fora da tela (clipping)
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
                pixels[rowOffset + x] = ((int)rStart << 16) | ((int)gStart << 8) | (int)bStart;
            }
            zStart += dZdx; 
            rStart += dRdx; 
            gStart += dGdx; 
            bStart += dBdx;
        }
    }
}