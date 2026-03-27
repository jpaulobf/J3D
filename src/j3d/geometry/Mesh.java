package j3d.geometry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Mesh class representing a 3D mesh with vertices and triangles.
 */
public class Mesh {

    // Vertices and triangles of the mesh
    public List<Vertex> vertices = new ArrayList<>();
    public List<Triangle> triangles = new ArrayList<>();

    public Mesh() {
    }

    public Mesh(List<Vertex> vertices, List<Triangle> triangles) {
        this.vertices = vertices;
        this.triangles = triangles;
    }

    /**
     * Factory method to create a cube mesh.
     */
    public static Mesh createCube() {
        Mesh m = new Mesh();
        for (int x = -1; x <= 1; x += 2)
            for (int y = -1; y <= 1; y += 2)
                for (int z = -1; z <= 1; z += 2)
                    m.vertices.add(new Vertex(x, y, z));
        int[][] f = { { 0, 1, 3 }, { 0, 3, 2 }, { 1, 5, 7 }, { 1, 7, 3 }, { 5, 4, 6 }, { 5, 6, 7 }, { 4, 0, 2 },
                { 4, 2, 6 }, { 2, 3, 7 }, { 2, 7, 6 }, { 4, 5, 1 }, { 4, 1, 0 } };
        Color[] c = { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN };
        for (int i = 0; i < 12; i++)
            m.triangles.add(new Triangle(f[i][0], f[i][1], f[i][2], c[i / 2]));
        return m;
    }

    /**
     * Factory method to create a wedge (ramp) mesh.
     */
    public static Mesh createWedge() {
        Mesh m = new Mesh();
        // Vertices (Base inferior e topo superior na parte de trás)
        m.vertices.add(new Vertex(-1, -1, -1)); // 0: Bottom Front Left
        m.vertices.add(new Vertex( 1, -1, -1)); // 1: Bottom Front Right
        m.vertices.add(new Vertex( 1, -1,  1)); // 2: Bottom Back Right
        m.vertices.add(new Vertex(-1, -1,  1)); // 3: Bottom Back Left
        m.vertices.add(new Vertex(-1,  1,  1)); // 4: Top Back Left
        m.vertices.add(new Vertex( 1,  1,  1)); // 5: Top Back Right

        Color c = Color.GRAY;
        // Triângulos com Winding Order corrigido para apontar para fora
        m.triangles.add(new Triangle(0, 1, 2, c)); // Bottom 1
        m.triangles.add(new Triangle(0, 2, 3, c)); // Bottom 2
        m.triangles.add(new Triangle(2, 3, 4, c)); // Back 1
        m.triangles.add(new Triangle(2, 4, 5, c)); // Back 2
        m.triangles.add(new Triangle(0, 5, 1, c)); // Slope 1 (Corrigido para o centro não sumir)
        m.triangles.add(new Triangle(0, 4, 5, c)); // Slope 2 (Corrigido para o centro não sumir)
        m.triangles.add(new Triangle(0, 3, 4, c)); // Left Side
        m.triangles.add(new Triangle(1, 5, 2, c)); // Right Side
        return m;
    }

    /**
     * Factory method to create a pyramid mesh.
     * 
     * @return
     */
    public static Mesh createPyramid() {
        Mesh m = new Mesh();
        m.vertices.add(new Vertex(-1, -1, -1));
        m.vertices.add(new Vertex(1, -1, -1));
        m.vertices.add(new Vertex(1, -1, 1));
        m.vertices.add(new Vertex(-1, -1, 1));
        m.vertices.add(new Vertex(0, 1, 0));
        m.triangles.add(new Triangle(0, 1, 4, Color.ORANGE));
        m.triangles.add(new Triangle(1, 2, 4, Color.PINK));
        m.triangles.add(new Triangle(2, 3, 4, Color.WHITE));
        m.triangles.add(new Triangle(3, 0, 4, Color.LIGHT_GRAY));
        m.triangles.add(new Triangle(0, 2, 1, Color.GRAY));
        m.triangles.add(new Triangle(0, 3, 2, Color.GRAY));

        for (Triangle t : m.triangles) {
            int temp = t.v2;
            t.v2 = t.v3;
            t.v3 = temp;
        }

        return m;
    }

    /**
     * Factory method to create a sphere mesh.
     * 
     * @param radius
     * @param latLines
     * @param lonLines
     * @return
     */
    public static Mesh createSphere(double radius, int latLines, int lonLines) {
        Mesh m = new Mesh();
        for (int i = 0; i <= latLines; i++) {
            double phi = Math.PI * i / latLines;
            for (int j = 0; j <= lonLines; j++) {
                double theta = 2 * Math.PI * j / lonLines;
                m.vertices.add(new Vertex(radius * Math.sin(phi) * Math.cos(theta), radius * Math.cos(phi),
                        radius * Math.sin(phi) * Math.sin(theta)));
            }
        }
        for (int i = 0; i < latLines; i++) {
            for (int j = 0; j < lonLines; j++) {
                int f = (i * (lonLines + 1)) + j;
                int s = f + lonLines + 1;
                m.triangles.add(new Triangle(f, s, f + 1, Color.YELLOW));
                m.triangles.add(new Triangle(s, s + 1, f + 1, Color.YELLOW));
            }
        }
        return m;
    }

    /**
     * Factory method to create a grid mesh.
     * 
     * @param size
     * @param tileSize
     * @return
     */
    public static Mesh createGrid(int size, double tileSize) {
        Mesh m = new Mesh();
        int half = size / 2;
        for (int z = -half; z <= half; z++) {
            for (int x = -half; x <= half; x++) {
                m.vertices.add(new Vertex(x * tileSize, 0, z * tileSize));
            }
        }
        int row = size + 1;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int i = z * row + x;
                Color col = (x + z) % 2 == 0 ? Color.WHITE : new Color(30, 30, 30); // Checkered Flag Style
                m.triangles.add(new Triangle(i, i + row, i + 1, col));
                m.triangles.add(new Triangle(i + 1, i + row, i + row + 1, col));
            }
        }

        // --- Extrusion to create the Box ---
        double height = 1.0; // Floor thickness
        double min = -half * tileSize;
        double max = half * tileSize;
        Color sideColor = new Color(30, 30, 30); // Dark gray for sides

        // Adds the 4 base vertices
        int baseIdx = m.vertices.size();
        m.vertices.add(new Vertex(min, -height, min)); // 0: Back-Left (Base)
        m.vertices.add(new Vertex(max, -height, min)); // 1: Back-Right (Base)
        m.vertices.add(new Vertex(max, -height, max)); // 2: Front-Right (Base)
        m.vertices.add(new Vertex(min, -height, max)); // 3: Front-Left (Base)

        // Top surface corner indices already exist
        int topBL = 0; // Back-Left (Top)
        int topBR = size; // Back-Right (Top)
        int topFL = size * row; // Front-Left (Top)
        int topFR = size * row + size; // Front-Right (Top)

        // Back Face (Z = min)
        m.triangles.add(new Triangle(topBL, topBR, baseIdx + 0, sideColor));
        m.triangles.add(new Triangle(baseIdx + 0, topBR, baseIdx + 1, sideColor));

        // Front Face (Z = max)
        m.triangles.add(new Triangle(topFL, baseIdx + 3, topFR, sideColor));
        m.triangles.add(new Triangle(baseIdx + 3, baseIdx + 2, topFR, sideColor));

        // Left Face (X = min)
        m.triangles.add(new Triangle(topBL, baseIdx + 0, topFL, sideColor));
        m.triangles.add(new Triangle(baseIdx + 0, baseIdx + 3, topFL, sideColor));

        // Right Face (X = max)
        m.triangles.add(new Triangle(topBR, topFR, baseIdx + 1, sideColor));
        m.triangles.add(new Triangle(baseIdx + 1, topFR, baseIdx + 2, sideColor));

        // Bottom Face
        m.triangles.add(new Triangle(baseIdx + 0, baseIdx + 1, baseIdx + 2, sideColor));
        m.triangles.add(new Triangle(baseIdx + 0, baseIdx + 2, baseIdx + 3, sideColor));

        return m;
    }
}