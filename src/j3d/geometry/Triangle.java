package j3d.geometry;

import java.awt.Color;

/**
 * Triangle class representing a triangle defined by three vertex indices and a
 * base color.
 */
public class Triangle {

    // Indices of the vertices that form the triangle and its base color
    public int v1, v2, v3;
    public Color baseColor;

    /**
     * Constructor for Triangle.
     */
    public Triangle(int a, int b, int c, Color col) {
        this.v1 = a;
        this.v2 = b;
        this.v3 = c;
        this.baseColor = col;
    }
}