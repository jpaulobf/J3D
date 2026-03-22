package j3d.lighting;

import java.awt.Color;
import j3d.geometry.Vertex;

/**
 * PointLight class representing a point light source in 3D space.
 */
public class PointLight {

    // Position, color, and intensity of the point light
    public Vertex pos;
    public Color color;
    public double intensity;

    /**
     * Constructor for PointLight.
     * 
     * @param x
     * @param y
     * @param z
     * @param col
     * @param intensity
     */
    public PointLight(double x, double y, double z, Color col, double intensity) {
        this.pos = new Vertex(x, y, z);
        this.color = col;
        this.intensity = intensity;
    }
}