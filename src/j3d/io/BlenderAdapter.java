package j3d.io;

import java.awt.Color;
import j3d.geometry.Mesh;
import j3d.geometry.Vertex;

/**
 * Classe utilitária para carregar modelos exportados do Blender.
 * O Blender nativamente usa o eixo Z como "cima" (Z-Up), enquanto o J3D usa Y (Y-Up).
 * Esta classe carrega o OBJ e converte automaticamente o sistema de coordenadas.
 */
public class BlenderAdapter {

    /**
     * Carrega um arquivo .obj exportado do Blender e corrige a orientação dos eixos.
     */
    public static Mesh adapt(String filePath, Color color) {
        // 1. Carrega a geometria bruta usando o loader existente
        Mesh mesh = ObjLoader.load(filePath, color);

        // 2. Converte de Z-Up (Blender) para Y-Up (J3D)
        // Isso equivale a uma rotação de -90 graus no eixo X: (x, y, z) -> (x, z, -y)
        for (Vertex v : mesh.vertices) {
            double tempY = v.y;
            v.y = v.z;
            v.z = -tempY;
        }

        return mesh;
    }
}
