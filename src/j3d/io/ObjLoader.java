package j3d.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import j3d.graphics.Texture;
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
        List<Double[]> rawVertices = new ArrayList<>();
        List<Double[]> rawUVs = new ArrayList<>();
        Map<String, Integer> vertexCache = new HashMap<>(); // Cache para evitar duplicatas: "idxV/idxVT" -> index
        Map<String, Color> materials = new HashMap<>();
        Map<String, Texture> textures = new HashMap<>();
        Color currentColor = fallbackColor;
        Texture currentTexture = null;

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
                    loadMaterials(parentDir + mtlFileName, materials, textures);
                }
                // 2. TROCAR DE COR
                else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    // Se o material existir no mapa, usa ele. Se não, mantém o fallback.
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                    currentTexture = textures.get(mtlName);
                }
                // 3. LER VÉRTICES
                else if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    rawVertices.add(new Double[]{
                        Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3])
                    });
                }
                // LER UVS
                else if (line.startsWith("vt ")) {
                    String[] tokens = line.split("\\s+");
                    rawUVs.add(new Double[]{
                        Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2])
                    });
                }
                // 4. LER FACES (TRIÂNGULOS / QUADS)
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");

                    int v1 = processVertex(tokens[1], rawVertices, rawUVs, mesh.vertices, vertexCache);
                    int v2 = processVertex(tokens[2], rawVertices, rawUVs, mesh.vertices, vertexCache);
                    int v3 = processVertex(tokens[3], rawVertices, rawUVs, mesh.vertices, vertexCache);

                    // Criamos o triângulo aplicando a 'currentColor' atual
                    mesh.triangles.add(new Triangle(v1, v2, v3, currentColor, currentTexture));

                    // Triangulação automática para Quads
                    if (tokens.length > 4) {
                        int v4 = processVertex(tokens[4], rawVertices, rawUVs, mesh.vertices, vertexCache);
                        mesh.triangles.add(new Triangle(v1, v3, v4, currentColor, currentTexture));
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
        
        List<Double[]> rawVertices = new ArrayList<>();
        List<Double[]> rawUVs = new ArrayList<>();
        List<Vertex> globalVertices = new ArrayList<>(); // Vértices finais (V + VT)
        Map<String, Integer> vertexCache = new HashMap<>(); // Cache "vIdx/vtIdx" -> globalIndex
        
        // Mapa de Grupo -> Lista de Triângulos (usando índices globais)
        Map<String, List<Triangle>> groups = new HashMap<>();
        Map<String, Color> materials = new HashMap<>();
        Map<String, Texture> textures = new HashMap<>();
        
        String currentGroup = "default";
        Color currentColor = fallbackColor;
        Texture currentTexture = null;

        File objFile = new File(filePath);
        String parentDir = objFile.getParent() == null ? "" : objFile.getParent() + File.separator;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            groups.put(currentGroup, new ArrayList<>());

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("mtllib ")) {
                    loadMaterials(parentDir + line.split("\\s+")[1], materials, textures);
                } 
                else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                    currentTexture = textures.get(mtlName);
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
                    rawVertices.add(new Double[]{
                        Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3])
                    });
                }
                else if (line.startsWith("vt ")) {
                    String[] tokens = line.split("\\s+");
                    rawUVs.add(new Double[]{
                        Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2])
                    });
                }
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");
                    int v1 = processVertex(tokens[1], rawVertices, rawUVs, globalVertices, vertexCache);
                    int v2 = processVertex(tokens[2], rawVertices, rawUVs, globalVertices, vertexCache);
                    int v3 = processVertex(tokens[3], rawVertices, rawUVs, globalVertices, vertexCache);

                    // Adiciona o triângulo à lista do grupo atual
                    groups.get(currentGroup).add(new Triangle(v1, v2, v3, currentColor, currentTexture));

                    if (tokens.length > 4) {
                        int v4 = processVertex(tokens[4], rawVertices, rawUVs, globalVertices, vertexCache);
                        groups.get(currentGroup).add(new Triangle(v1, v3, v4, currentColor, currentTexture));
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
                    
                    localTris.add(new Triangle(localV1, localV2, localV3, t.baseColor, t.texture));
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

    // Converte a string de índice "v/vt/vn" em um Vertex real com UVs e gerencia o cache
    private static int processVertex(String token, List<Double[]> rawV, List<Double[]> rawVT, List<Vertex> finalVertices, Map<String, Integer> cache) {
        if (cache.containsKey(token)) return cache.get(token);

        String[] parts = token.split("/");
        int vIdx = Integer.parseInt(parts[0]) - 1;
        int vtIdx = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) - 1 : -1;

        Double[] pos = rawV.get(vIdx);
        Vertex v = new Vertex(pos[0], pos[1], pos[2]);

        if (vtIdx >= 0) {
            Double[] uv = rawVT.get(vtIdx);
            v.u = uv[0];
            v.v = uv[1];
        }

        finalVertices.add(v);
        int newIdx = finalVertices.size() - 1;
        cache.put(token, newIdx);
        return newIdx;
    }

    // Auxiliar para remapear vértices globais para locais
    private static int mapVertex(int globalIdx, List<Vertex> globalData, List<Vertex> localData, Map<Integer, Integer> map) {
        if (map.containsKey(globalIdx)) return map.get(globalIdx);
        Vertex v = globalData.get(globalIdx);
        Vertex newV = new Vertex(v.x, v.y, v.z);
        newV.u = v.u; // Clona UV
        newV.v = v.v;
        localData.add(newV);
        int newIdx = localData.size() - 1;
        map.put(globalIdx, newIdx);
        return newIdx;
    }

    /**
     * Lê o arquivo .mtl e extrai as cores Diffuse (Kd)
     */
    private static void loadMaterials(String mtlPath, Map<String, Color> materials, Map<String, Texture> textures) {
        File mtlFile = new File(mtlPath);
        String parentDir = mtlFile.getParent() == null ? "" : mtlFile.getParent() + File.separator;

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
                else if (line.startsWith("map_Kd ")) {
                    // Carrega a textura
                    String texFile = line.split("\\s+")[1];
                    textures.put(currentMtlName, new Texture(parentDir + texFile));
                }
            }
            System.out.println("Materiais carregados do arquivo: " + mtlPath);
        } catch (Exception e) {
            System.err.println("Aviso: Arquivo MTL não encontrado ou erro de leitura: " + mtlPath);
        }
    }
}