package j3d.physics;

import java.util.List;
import j3d.core.GameObject;

/**
 * PhysicsEngine class responsible for handling collision detection and
 * physics-related calculations in the game.
 */
public class PhysicsEngine {

    public static final double PLAYER_RADIUS = 0.5;
    public static final double PLAYER_HEIGHT = 1.8;
    public static final double PLAYER_EYE_HEIGHT = 1.6;

    /**
     * Verifica se uma posição futura do jogador colide com a geometria do mundo.
     */
    public boolean checkPlayerCollision(double targetX, double targetY, double targetZ, List<GameObject> worldObjects) {
        double feetY = targetY - PLAYER_EYE_HEIGHT;
        double headY = feetY + PLAYER_HEIGHT;

        for (GameObject obj : worldObjects) {
            // Ignoramos objetos que não têm colisão (como o seu chão)
            if (!obj.hasCollision)
                continue;

            if (this.checkCollision(targetX, targetZ, feetY, headY, PLAYER_RADIUS, obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se uma posição (geralmente a câmera) colide com este objeto.
     * A verificação é feita usando AABB (Axis-Aligned Bounding Box).
     * 
     * @param x          Posição X da entidade
     * @param z          Posição Z da entidade
     * @param entityMinY Posição Y da base da entidade (pés)
     * @param entityMaxY Posição Y do topo da entidade (cabeça)
     * @param radius     Raio da entidade (usado para definir a caixa do jogador)
     * @return true se houver colisão
     */
    public boolean checkCollision(double x, double z, double entityMinY, double entityMaxY, double radius,
            GameObject object) {
        if (!object.hasCollision)
            return false;

        // 1. Define o AABB do jogador (entidade)
        double playerMinX = x - radius;
        double playerMaxX = x + radius;
        double playerMinZ = z - radius;
        double playerMaxZ = z + radius;

        // 2. Define o AABB do objeto no espaço do mundo
        double objMinX = object.transform.x + object.minX * object.transform.scaleX;
        double objMaxX = object.transform.x + object.maxX * object.transform.scaleX;
        double objMinY = object.transform.y + object.minY * object.transform.scaleY;
        double objMaxY = object.transform.y + object.maxY * object.transform.scaleY;
        double objMinZ = object.transform.z + object.minZ * object.transform.scaleZ;
        double objMaxZ = object.transform.z + object.maxZ * object.transform.scaleZ;

        // 3. Verifica a sobreposição em todos os 3 eixos. Há colisão se todos se sobrepõem.
        return (playerMinX < objMaxX && playerMaxX > objMinX) &&
               (entityMinY < objMaxY && entityMaxY > objMinY) &&
               (playerMinZ < objMaxZ && playerMaxZ > objMinZ);
    }

    /**
     * Verifica colisão AABB (Axis-Aligned Bounding Box) entre dois GameObjects.
     */
    public boolean checkObjectCollision(GameObject a, GameObject b) {
        if (!a.hasCollision || !b.hasCollision)
            return false;

        double aMinX = a.transform.x + a.minX * a.transform.scaleX;
        double aMaxX = a.transform.x + a.maxX * a.transform.scaleX;
        double aMinY = a.transform.y + a.minY * a.transform.scaleY;
        double aMaxY = a.transform.y + a.maxY * a.transform.scaleY;
        double aMinZ = a.transform.z + a.minZ * a.transform.scaleZ;
        double aMaxZ = a.transform.z + a.maxZ * a.transform.scaleZ;

        double bMinX = b.transform.x + b.minX * b.transform.scaleX;
        double bMaxX = b.transform.x + b.maxX * b.transform.scaleX;
        double bMinY = b.transform.y + b.minY * b.transform.scaleY;
        double bMaxY = b.transform.y + b.maxY * b.transform.scaleY;
        double bMinZ = b.transform.z + b.minZ * b.transform.scaleZ;
        double bMaxZ = b.transform.z + b.maxZ * b.transform.scaleZ;

        return (aMinX < bMaxX && aMaxX > bMinX) &&
               (aMinY < bMaxY && aMaxY > bMinY) &&
               (aMinZ < bMaxZ && aMaxZ > bMinZ);
    }
}
