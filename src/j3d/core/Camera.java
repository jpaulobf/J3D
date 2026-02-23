package j3d.core;

import j3d.math.Matrix4;
import j3d.math.Transform;

/**
 * Camera class representing the position and orientation of the camera in 3D
 * space.
 */
public class Camera {

    // Transform representing the camera's position and rotation
    public Transform transform = new Transform();
    public double yaw = 0, pitch = 0;

    /**
     * Computes the view matrix for this camera, combining rotation and translation.
     * 
     * @return
     */
    public Matrix4 getViewMatrix() {
        double cY = Math.cos(yaw), sY = Math.sin(yaw), cP = Math.cos(pitch), sP = Math.sin(pitch);
        Matrix4 rY = new Matrix4();
        rY.m[0][0] = cY;
        rY.m[0][2] = sY;
        rY.m[2][0] = -sY;
        rY.m[2][2] = cY;
        Matrix4 rX = new Matrix4();
        rX.m[1][1] = cP;
        rX.m[1][2] = -sP;
        rX.m[2][1] = sP;
        rX.m[2][2] = cP;
        Matrix4 t = new Matrix4();
        t.m[0][3] = -transform.x;
        t.m[1][3] = -transform.y;
        t.m[2][3] = -transform.z;
        return Matrix4.multiply(Matrix4.multiply(rX, rY), t);
    }
}