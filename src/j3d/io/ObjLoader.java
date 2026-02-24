package j3d.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import j3d.geometry.Mesh;
import j3d.geometry.Triangle;
import j3d.math.Vertex;

public class ObjLoader {

    /**
     * Carrega um ficheiro .obj e converte-o numa Mesh compatível com o motor.
     * * @param filePath Caminho para o ficheiro (ex: "res/nave.obj")
     * @param color Cor base que será aplicada a todas as faces do modelo
     * @return Uma instância de Mesh pronta a ser usada num GameObject
     */
    public static Mesh load(String filePath, Color color) {
        Mesh mesh = new Mesh();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // 1. Lê os vértices (Pontos 3D)
                if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    mesh.vertices.add(new Vertex(x, y, z));
                } 
                // 2. Lê as faces (Conexões)
                else if (line.startsWith("f ")) {
                    String[] tokens = line.split("\\s+");
                    
                    // O formato OBJ suporta "v", "v/vt" ou "v/vt/vn". 
                    // O split("/") garante que pegamos apenas o índice do vértice.
                    // O OBJ começa a contar do índice 1, o Java começa do 0, por isso subtraímos 1.
                    int v1 = Integer.parseInt(tokens[1].split("/")[0]) - 1;
                    int v2 = Integer.parseInt(tokens[2].split("/")[0]) - 1;
                    int v3 = Integer.parseInt(tokens[3].split("/")[0]) - 1;

                    mesh.triangles.add(new Triangle(v1, v2, v3, color));

                    // Triangulação automática:
                    // Se o modelo vier com "Quads" (faces de 4 lados), 
                    // dividimos em dois triângulos automaticamente.
                    if (tokens.length > 4) {
                        int v4 = Integer.parseInt(tokens[4].split("/")[0]) - 1;
                        mesh.triangles.add(new Triangle(v1, v3, v4, color));
                    }
                }
            }
            System.out.println("Modelo carregado: " + filePath + " | Vértices: " + mesh.vertices.size() + " | Faces: " + mesh.triangles.size());

        } catch (Exception e) {
            System.err.println("Erro ao carregar o modelo OBJ: " + filePath);
            e.printStackTrace();
        }

        return mesh;
    }
}