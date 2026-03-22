package j3d.io;

import java.awt.Color;
import j3d.geometry.Mesh;
import j3d.geometry.Vertex;

/**
 * Utility class to load models exported from Blender.
 * Blender natively uses the Z-axis as "up" (Z-Up), while J3D uses Y (Y-Up).
 * This class loads the OBJ and automatically converts the coordinate system.
 */
public class BlenderAdapter {

    /**
     * Loads an .obj file exported from Blender and corrects axis orientation.
     * 
     * @param filePath The path to the OBJ file.
     * @param color    The fallback color if no material is found.
     * @return The mesh with corrected coordinates.
     */
    public static Mesh adapt(String filePath, Color color) {
        // 1. Loads raw geometry using the existing loader
        Mesh mesh = ObjLoader.load(filePath, color);

        // 2. Converts from Z-Up (Blender) to Y-Up (J3D)
        // This is equivalent to a -90 degree rotation on the X axis: (x, y, z) -> (x,
        // z, -y)
        for (Vertex v : mesh.vertices) {
            double tempY = v.y;
            v.y = v.z;
            v.z = -tempY;
        }

        return mesh;
    }
}
