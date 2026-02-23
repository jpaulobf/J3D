package geometry;

/**
 * Vertex class representing a point in 3D space with homogeneous coordinates.
 */
public class Vertex {
    // x, y, z coordinates and homogeneous w coordinate
    public double x, y, z, w = 1.0;

    /**
     * Constructor for Vertex.
     * @param x
     * @param y
     * @param z
     */
    public Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}