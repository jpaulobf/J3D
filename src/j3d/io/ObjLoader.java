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
        Map<String, Integer> vertexCache = new HashMap<>(); // Cache to avoid duplicates: "idxV/idxVT" -> index
        Map<String, Color> materials = new HashMap<>();
        Map<String, Texture> textures = new HashMap<>();
        Color currentColor = fallbackColor;
        Texture currentTexture = null;

        // Tries to discover base directory to look for .mtl file in same place as .obj
        File objFile = new File(filePath);
        String parentDir = objFile.getParent() == null ? "" : objFile.getParent() + File.separator;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // 1. READ MATERIAL LIBRARY (.mtl)
                if (line.startsWith("mtllib ")) {
                    String mtlFileName = line.split("\\s+")[1];
                    loadMaterials(parentDir + mtlFileName, materials, textures);
                }
                // 2. CHANGE COLOR
                else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    // If material exists in map, use it. If not, keep fallback.
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                    currentTexture = textures.get(mtlName);
                }
                // 3. READ VERTICES
                else if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    rawVertices.add(new Double[] {
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2]),
                            Double.parseDouble(tokens[3])
                    });
                }
                // READ UVS
                else if (line.startsWith("vt ")) {
                    String[] tokens = line.split("\\s+");
                    rawUVs.add(new Double[] {
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2])
                    });
                }
                // 4. READ FACES (TRIANGLES / QUADS)
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");

                    int v1 = processVertex(tokens[1], rawVertices, rawUVs, mesh.vertices, vertexCache);
                    int v2 = processVertex(tokens[2], rawVertices, rawUVs, mesh.vertices, vertexCache);
                    int v3 = processVertex(tokens[3], rawVertices, rawUVs, mesh.vertices, vertexCache);

                    // Create triangle applying current 'currentColor'
                    mesh.triangles.add(new Triangle(v1, v2, v3, currentColor, currentTexture));

                    // Automatic triangulation for Quads
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
     * Loads an OBJ file and separates groups ('g' or 'o' tags) into distinct
     * GameObjects.
     * This allows each part of the model to have its own collision box (AABB).
     */
    public static List<GameObject> loadScene(String filePath, Color fallbackColor) {
        List<GameObject> objects = new ArrayList<>();

        List<Double[]> rawVertices = new ArrayList<>();
        List<Double[]> rawUVs = new ArrayList<>();
        List<Vertex> globalVertices = new ArrayList<>(); // Final vertices (V + VT)
        Map<String, Integer> vertexCache = new HashMap<>(); // Cache "vIdx/vtIdx" -> globalIndex

        // Group Map -> Triangle List (using global indices)
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
                } else if (line.startsWith("usemtl ")) {
                    String mtlName = line.split("\\s+")[1];
                    currentColor = materials.getOrDefault(mtlName, fallbackColor);
                    currentTexture = textures.get(mtlName);
                }
                // Detects object or group switch
                else if (line.startsWith("o ") || line.startsWith("g ")) {
                    String rawName = line.substring(2).trim();
                    currentGroup = rawName;

                    // Ensures unique names. If "cube" already exists, creates "cube_1", "cube_2",
                    // etc.
                    int id = 1;
                    while (groups.containsKey(currentGroup)) {
                        currentGroup = rawName + "_" + id++;
                    }
                    groups.put(currentGroup, new ArrayList<>());
                } else if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    rawVertices.add(new Double[] {
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2]),
                            Double.parseDouble(tokens[3])
                    });
                } else if (line.startsWith("vt ")) {
                    String[] tokens = line.split("\\s+");
                    rawUVs.add(new Double[] {
                            Double.parseDouble(tokens[1]),
                            Double.parseDouble(tokens[2])
                    });
                } else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");
                    int v1 = processVertex(tokens[1], rawVertices, rawUVs, globalVertices, vertexCache);
                    int v2 = processVertex(tokens[2], rawVertices, rawUVs, globalVertices, vertexCache);
                    int v3 = processVertex(tokens[3], rawVertices, rawUVs, globalVertices, vertexCache);

                    // Adds triangle to current group list
                    groups.get(currentGroup).add(new Triangle(v1, v2, v3, currentColor, currentTexture));

                    if (tokens.length > 4) {
                        int v4 = processVertex(tokens[4], rawVertices, rawUVs, globalVertices, vertexCache);
                        groups.get(currentGroup).add(new Triangle(v1, v3, v4, currentColor, currentTexture));
                    }
                }
            }

            // Processes groups to create individual GameObjects
            for (Map.Entry<String, List<Triangle>> entry : groups.entrySet()) {
                List<Triangle> groupTris = entry.getValue();
                if (groupTris.isEmpty())
                    continue;

                List<Vertex> localVertices = new ArrayList<>();
                List<Triangle> localTris = new ArrayList<>();
                Map<Integer, Integer> globalToLocalMap = new HashMap<>();

                for (Triangle t : groupTris) {
                    // Remaps global indices to locals for this new Mesh
                    int localV1 = mapVertex(t.v1, globalVertices, localVertices, globalToLocalMap);
                    int localV2 = mapVertex(t.v2, globalVertices, localVertices, globalToLocalMap);
                    int localV3 = mapVertex(t.v3, globalVertices, localVertices, globalToLocalMap);

                    localTris.add(new Triangle(localV1, localV2, localV3, t.baseColor, t.texture));
                }

                // Creates object only with vertices it uses (generates correct AABB)
                objects.add(new GameObject(new Mesh(localVertices, localTris)));
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar cena OBJ: " + filePath);
            e.printStackTrace();
        }
        return objects;
    }

    // Converts index string "v/vt/vn" into real Vertex with UVs and manages cache
    private static int processVertex(String token, List<Double[]> rawV, List<Double[]> rawVT,
            List<Vertex> finalVertices, Map<String, Integer> cache) {
        if (cache.containsKey(token))
            return cache.get(token);

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

    // Helper to remap global vertices to local ones
    private static int mapVertex(int globalIdx, List<Vertex> globalData, List<Vertex> localData,
            Map<Integer, Integer> map) {
        if (map.containsKey(globalIdx))
            return map.get(globalIdx);
        Vertex v = globalData.get(globalIdx);
        Vertex newV = new Vertex(v.x, v.y, v.z);
        newV.u = v.u; // Clone UV
        newV.v = v.v;
        localData.add(newV);
        int newIdx = localData.size() - 1;
        map.put(globalIdx, newIdx);
        return newIdx;
    }

    /**
     * Reads .mtl file and extracts Diffuse colors (Kd)
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
                    // Kd defines RGB color (values 0.0 to 1.0)
                    String[] tokens = line.split("\\s+");
                    float r = Float.parseFloat(tokens[1]);
                    float g = Float.parseFloat(tokens[2]);
                    float b = Float.parseFloat(tokens[3]);
                    materials.put(currentMtlName, new Color(r, g, b));
                } else if (line.startsWith("map_Kd ")) {
                    // Loads texture
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