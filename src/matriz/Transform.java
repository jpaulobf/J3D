package matriz;

import geometry.Matrix4;

/**
 * Transform class representing position and rotation of an object in 3D space.
 */
public class Transform {

    // Position (x, y, z) and rotation (rotX, rotY, rotZ) in radians
    public double x = 0, y = 0, z = 0, rotX = 0, rotY = 0, rotZ = 0;

    /**
    * Computes the model matrix for this transform, combining rotation and translation.
    * @return
    */
    public Matrix4 getModelMatrix() {
        double cx = Math.cos(rotX), sx = Math.sin(rotX), cy = Math.cos(rotY), sy = Math.sin(rotY), cz = Math.cos(rotZ),
                sz = Math.sin(rotZ);
        Matrix4 r = new Matrix4();
        r.m[0][0] = cy * cz;
        r.m[0][1] = -cy * sz;
        r.m[0][2] = sy;
        r.m[1][0] = sx * sy * cz + cx * sz;
        r.m[1][1] = -sx * sy * sz + cx * cz;
        r.m[1][2] = -sx * cy;
        r.m[2][0] = -cx * sy * cz + sx * sz;
        r.m[2][1] = cx * sy * sz + sx * cz;
        r.m[2][2] = cx * cy;
        Matrix4 t = new Matrix4();
        t.m[0][3] = x;
        t.m[1][3] = y;
        t.m[2][3] = z;
        return Matrix4.multiply(t, r);
    }
}
