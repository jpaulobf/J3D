package j3d.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import j3d.geometry.Mesh;
import j3d.geometry.Triangle;
import j3d.geometry.Vertex;

/**
 * ObjLoader class responsible for loading 3D models from .obj files, including
 * support for .mtl material files to extract colors.
 */
public class ObjLoader {

    /**
     * Loads a 3D model from an OBJ file, applying colors from an associated MTL
     * file if available.
     * 
     * @param filePath
     * @param fallbackColor
     * @return
     */
    public static Mesh load(String filePath, Color fallbackColor) {
        Mesh mesh = new Mesh();
        Map<String, Color> materials = new HashMap<>();
        Color currentColor = fallbackColor;

        // Tenta descobrir o diretório base para procurar o arquivo .mtl no mesmo lugar
        // do .obj
        File objFile = new File(filePath);
        String parentDir = objFile.getParent() == null ? "" : objFile.getParent() + File.separator;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // 1. LER BIBLIOTECA DE MATERIAIS (.mtl)
                if (line.startsWith("mtllib ")) {
                    String mtlFileName = line.split("\\s+")[1];
                    loadMaterials(parentDir + mtlFileName, materials);
                }
                // 2. TROCAR DE COR
                else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    // Se o material existir no mapa, usa ele. Se não, mantém o fallback.
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                }
                // 3. LER VÉRTICES
                else if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    mesh.vertices.add(new Vertex(x, y, z));
                }
                // 4. LER FACES (TRIÂNGULOS / QUADS)
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");

                    int v1 = Integer.parseInt(tokens[1].split("/")[0]) - 1;
                    int v2 = Integer.parseInt(tokens[2].split("/")[0]) - 1;
                    int v3 = Integer.parseInt(tokens[3].split("/")[0]) - 1;

                    // Criamos o triângulo aplicando a 'currentColor' atual
                    mesh.triangles.add(new Triangle(v1, v2, v3, currentColor));

                    // Triangulação automática para Quads
                    if (tokens.length > 4) {
                        int v4 = Integer.parseInt(tokens[4].split("/")[0]) - 1;
                        mesh.triangles.add(new Triangle(v1, v3, v4, currentColor));
                    }
                }
            }
            System.out.println("Modelo carregado: " + filePath + " | Materiais: " + materials.size() + " | Vértices: "
                    + mesh.vertices.size() + " | Faces: " + mesh.triangles.size());

        } catch (Exception e) {
            System.err.println("Erro ao carregar o modelo OBJ: " + filePath);
            e.printStackTrace();
        }

        return mesh;
    }

    /**
     * Lê o arquivo .mtl e extrai as cores Diffuse (Kd)
     */
    private static void loadMaterials(String mtlPath, Map<String, Color> materials) {
        try (BufferedReader br = new BufferedReader(new FileReader(mtlPath))) {
            String line;
            String currentMtlName = "";

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("newmtl ")) {
                    currentMtlName = line.split("\\s+")[1];
                } else if (line.startsWith("Kd ")) {
                    // Kd define a cor RGB (valores de 0.0 a 1.0)
                    String[] tokens = line.split("\\s+");
                    float r = Float.parseFloat(tokens[1]);
                    float g = Float.parseFloat(tokens[2]);
                    float b = Float.parseFloat(tokens[3]);
                    materials.put(currentMtlName, new Color(r, g, b));
                }
            }
            System.out.println("Materiais carregados do arquivo: " + mtlPath);
        } catch (Exception e) {
            System.err.println("Aviso: Arquivo MTL não encontrado ou erro de leitura: " + mtlPath);
        }
    }
}