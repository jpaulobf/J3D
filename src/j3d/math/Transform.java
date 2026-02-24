package j3d.math;

/**
 * Transform class representing position, rotation, and scale of an object in 3D space.
 */
public class Transform {

    // Position (x, y, z) and rotation (rotX, rotY, rotZ) in radians
    public double x = 0, y = 0, z = 0, rotX = 0, rotY = 0, rotZ = 0;
    
    // Scale properties initialized to 1.0 (tamanho original)
    public double scaleX = 1.0, scaleY = 1.0, scaleZ = 1.0;

    /**
     * Computes the model matrix for this transform, combining scale, rotation, and translation.
     * @return Matrix4
     */
    public Matrix4 getModelMatrix() {
        // 1. Matriz de Escala (Scale)
        Matrix4 s = new Matrix4();
        s.m[0][0] = scaleX;
        s.m[1][1] = scaleY;
        s.m[2][2] = scaleZ;

        // 2. Matriz de Rotação (Rotation)
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

        // 3. Matriz de Translação (Translation)
        Matrix4 t = new Matrix4();
        t.m[0][3] = x;
        t.m[1][3] = y;
        t.m[2][3] = z;

        // A ordem matemática (Scale -> Rotate -> Translate) aplicada da direita para a esquerda
        Matrix4 rs = Matrix4.multiply(r, s);
        return Matrix4.multiply(t, rs);
    }
    
    /**
     * Método de conveniência para alterar a escala de todos os eixos uniformemente.
     * @param scale O novo tamanho (ex: 2.0 para dobrar o tamanho, 0.5 para diminuir pela metade)
     */
    public void setScale(double scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
    }
}