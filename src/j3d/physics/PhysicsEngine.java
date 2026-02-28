package j3d.physics;

import java.util.List;
import j3d.core.GameObject;

/**
 * PhysicsEngine class responsible for handling collision detection and physics-related calculations in the game.
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
            if (!obj.hasCollision) continue;

            if (this.checkCollision(targetX, targetZ, feetY, headY, PLAYER_RADIUS, obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se uma posição (geralmente a câmera) colide com este objeto.
     * @param x Posição X da entidade
     * @param z Posição Z da entidade
     * @param entityMinY Posição Y da base da entidade (pés)
     * @param entityMaxY Posição Y do topo da entidade (cabeça)
     * @param radius Raio da entidade (tamanho do jogador)
     * @return true se houver colisão
     */
    public boolean checkCollision(double x, double z, double entityMinY, double entityMaxY, double radius, GameObject object) {
        if (!object.hasCollision) return false;
        
        // Verifica limites verticais (Intersecção de Intervalos)
        double worldMinY = object.transform.y + object.minY * object.transform.scaleY;
        double worldMaxY = object.transform.y + object.maxY * object.transform.scaleY;
        
        // Se o personagem está totalmente acima ou totalmente abaixo do objeto, não há colisão
        if (entityMinY >= worldMaxY || entityMaxY <= worldMinY) return false;

        double dx = x - object.transform.x;
        double dz = z - object.transform.z;
        double distSq = dx * dx + dz * dz;
        double scaledRadius = object.collisionRadius * Math.max(object.transform.scaleX, object.transform.scaleZ);
        double totalRadius = scaledRadius + radius;
        return distSq < (totalRadius * totalRadius);
    }
}
