package j3d.math;

import j3d.geometry.Vertex;

/**
 * Matrix4 class representing a 4x4 matrix for 3D transformations.
 */
public class Matrix4 {

    // 4x4 matrix for 3D transformations
    public double[][] m = new double[4][4];

    /**
     * Initializes the matrix as an identity matrix.
     */
    public Matrix4() {
        m[0][0] = m[1][1] = m[2][2] = m[3][3] = 1;
    }

    /**
     * Multiplies a 4x4 matrix with a vertex (homogeneous coordinates).
     * 
     * @param mat
     * @param v
     * @return
     */
    public static Vertex multiply(Matrix4 mat, Vertex v) {
        double x = v.x * mat.m[0][0] + v.y * mat.m[0][1] + v.z * mat.m[0][2] + v.w * mat.m[0][3];
        double y = v.x * mat.m[1][0] + v.y * mat.m[1][1] + v.z * mat.m[1][2] + v.w * mat.m[1][3];
        double z = v.x * mat.m[2][0] + v.y * mat.m[2][1] + v.z * mat.m[2][2] + v.w * mat.m[2][3];
        double w = v.x * mat.m[3][0] + v.y * mat.m[3][1] + v.z * mat.m[3][2] + v.w * mat.m[3][3];
        Vertex res = new Vertex(x, y, z);
        res.w = w;
        return res;
    }

    /**
     * Multiplies two 4x4 matrices.
     * 
     * @param a
     * @param b
     * @return
     */
    public static Matrix4 multiply(Matrix4 a, Matrix4 b) {
        Matrix4 r = new Matrix4();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                r.m[i][j] = a.m[i][0] * b.m[0][j] + a.m[i][1] * b.m[1][j] + a.m[i][2] * b.m[2][j]
                        + a.m[i][3] * b.m[3][j];
        return r;
    }

    /**
     * Creates a perspective projection matrix.
     * 
     * @param fov
     * @param asp
     * @param n
     * @param f
     * @return
     */
    public static Matrix4 projection(double fov, double asp, double n, double f) {
        Matrix4 r = new Matrix4();
        double t = 1.0 / Math.tan(Math.toRadians(fov) / 2);
        r.m[0][0] = t / asp;
        r.m[1][1] = t;
        r.m[2][2] = -(f + n) / (f - n);
        r.m[2][3] = -(2 * f * n) / (f - n);
        r.m[3][2] = -1;
        r.m[3][3] = 0;
        return r;
    }
}