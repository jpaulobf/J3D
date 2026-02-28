package j3d.geometry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import j3d.math.Vertex;

/**
 * Mesh class representing a 3D mesh with vertices and triangles.
 */
public class Mesh {

    // Vertices and triangles of the mesh
    public List<Vertex> vertices = new ArrayList<>();
    public List<Triangle> triangles = new ArrayList<>();

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
                Color col = (x + z) % 2 == 0 ? new Color(40, 40, 40) : new Color(60, 60, 60);
                m.triangles.add(new Triangle(i, i + row, i + 1, col));
                m.triangles.add(new Triangle(i + 1, i + row, i + row + 1, col));
            }
        }
        return m;
    }
}