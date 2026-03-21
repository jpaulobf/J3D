package j3d.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import j3d.geometry.Mesh;
import j3d.geometry.Triangle;
import j3d.geometry.Vertex;
import j3d.core.GameObject;

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
     * Carrega um arquivo OBJ e separa os grupos (tags 'g' ou 'o') em GameObjects distintos.
     * Isso permite que cada parte do modelo tenha sua própria caixa de colisão (AABB).
     */
    public static List<GameObject> loadScene(String filePath, Color fallbackColor) {
        List<GameObject> objects = new ArrayList<>();
        List<Vertex> globalVertices = new ArrayList<>();
        
        // Mapa de Grupo -> Lista de Triângulos (usando índices globais)
        Map<String, List<Triangle>> groups = new HashMap<>();
        Map<String, Color> materials = new HashMap<>();
        
        String currentGroup = "default";
        Color currentColor = fallbackColor;

        File objFile = new File(filePath);
        String parentDir = objFile.getParent() == null ? "" : objFile.getParent() + File.separator;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            groups.put(currentGroup, new ArrayList<>());

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("mtllib ")) {
                    loadMaterials(parentDir + line.split("\\s+")[1], materials);
                } 
                else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                }
                // Detecta troca de objeto ou grupo
                else if (line.startsWith("o ") || line.startsWith("g ")) {
                    String rawName = line.substring(2).trim();
                    currentGroup = rawName;
                    
                    // Garante nomes únicos. Se "cube" já existe, cria "cube_1", "cube_2", etc.
                    int id = 1;
                    while (groups.containsKey(currentGroup)) {
                        currentGroup = rawName + "_" + id++;
                    }
                    groups.put(currentGroup, new ArrayList<>());
                }
                else if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    globalVertices.add(new Vertex(
                        Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3])
                    ));
                }
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");
                    int v1 = Integer.parseInt(tokens[1].split("/")[0]) - 1;
                    int v2 = Integer.parseInt(tokens[2].split("/")[0]) - 1;
                    int v3 = Integer.parseInt(tokens[3].split("/")[0]) - 1;

                    // Adiciona o triângulo à lista do grupo atual
                    groups.get(currentGroup).add(new Triangle(v1, v2, v3, currentColor));

                    if (tokens.length > 4) {
                        int v4 = Integer.parseInt(tokens[4].split("/")[0]) - 1;
                        groups.get(currentGroup).add(new Triangle(v1, v3, v4, currentColor));
                    }
                }
            }

            // Processa os grupos para criar GameObjects individuais
            for (Map.Entry<String, List<Triangle>> entry : groups.entrySet()) {
                List<Triangle> groupTris = entry.getValue();
                if (groupTris.isEmpty()) continue;

                List<Vertex> localVertices = new ArrayList<>();
                List<Triangle> localTris = new ArrayList<>();
                Map<Integer, Integer> globalToLocalMap = new HashMap<>();

                for (Triangle t : groupTris) {
                    // Remapeia os índices globais para locais deste novo Mesh
                    int localV1 = mapVertex(t.v1, globalVertices, localVertices, globalToLocalMap);
                    int localV2 = mapVertex(t.v2, globalVertices, localVertices, globalToLocalMap);
                    int localV3 = mapVertex(t.v3, globalVertices, localVertices, globalToLocalMap);
                    
                    localTris.add(new Triangle(localV1, localV2, localV3, t.baseColor));
                }

                // Cria o objeto apenas com os vértices que ele usa (gera AABB correta)
                objects.add(new GameObject(new Mesh(localVertices, localTris)));
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar cena OBJ: " + filePath);
            e.printStackTrace();
        }
        return objects;
    }

    // Auxiliar para remapear vértices globais para locais
    private static int mapVertex(int globalIdx, List<Vertex> globalData, List<Vertex> localData, Map<Integer, Integer> map) {
        if (map.containsKey(globalIdx)) return map.get(globalIdx);
        Vertex v = globalData.get(globalIdx);
        // Clona o vértice para evitar referências compartilhadas indesejadas
        localData.add(new Vertex(v.x, v.y, v.z));
        int newIdx = localData.size() - 1;
        map.put(globalIdx, newIdx);
        return newIdx;
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