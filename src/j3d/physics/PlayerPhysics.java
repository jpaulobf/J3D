package j3d.physics;

import j3d.core.Camera;
import j3d.core.GameObject;
import java.util.List;

/**
 * Player Physics class
 */
public class PlayerPhysics {

    // Physics Engine
    private final PhysicsEngine engine;

    // Player Physical Constants
    public static final double PLAYER_RADIUS = 0.5;
    public static final double PLAYER_HEIGHT = 4.0;
    public static final double PLAYER_EYE_HEIGHT = 4.0;
    private final double GRAVITY = -25.0;
    private final double JUMP_FORCE = 10.0;
    private final double STEP_HEIGHT = 1.3;

    // State
    private double verticalVelocity = 0;
    private boolean isGrounded = false;

    /**
     * Constructor
     *
     * @param engine
     */
    public PlayerPhysics(PhysicsEngine engine) {
        this.engine = engine;
    }

    /**
     * Handle Physics for the player
     * 
     * @param deltaTime
     * @param camera
     * @param moveX
     * @param moveZ
     * @param jumpRequested
     * @param worldObjects
     */
    public void handlePhysics(double deltaTime, Camera camera, double moveX, double moveZ, boolean jumpRequested,
            List<GameObject> worldObjects) {
        // 1. Lógica de Pulo
        if (jumpRequested && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // 2. Movimento Horizontal
        // Processamos X e Z separadamente para permitir deslizar em quinas e paredes
        if (moveX != 0) {
            applyHorizontalMovement(moveX, 0, camera, worldObjects);
        }
        if (moveZ != 0) {
            applyHorizontalMovement(0, moveZ, camera, worldObjects);
        }

        // 3. Lógica de Chão e Gravidade (Mesh Aware)
        double currentFeetY = camera.transform.y - PLAYER_EYE_HEIGHT;
        
        // Encontra a altura do chão na posição exata (X, Z) do jogador
        double groundY = -Double.MAX_VALUE; 
        for (GameObject obj : worldObjects) {
            if (!obj.hasCollision) continue;
            
            double h = obj.getWorldHeightAt(camera.transform.x, camera.transform.z);
            
            // Consideramos o chão se estiver abaixo de nós ou dentro do alcance do Step Height
            if (h != -Double.MAX_VALUE && h <= (currentFeetY + STEP_HEIGHT + 0.01)) {
                if (h > groundY) groundY = h;
            }
        }
        
        if (groundY == -Double.MAX_VALUE) groundY = 0; // Piso base se nada for encontrado

        // Aplica Gravidade
        verticalVelocity += GRAVITY * deltaTime;
        double nextFeetY = currentFeetY + (verticalVelocity * deltaTime);

        // Colisão com o Chão (Piso, Escada ou Rampa)
        // Aumentamos a tolerância de "Snap" para evitar o efeito de andar no ar ao descer
        boolean isFalling = verticalVelocity < 0;
        double snapThreshold = isGrounded ? STEP_HEIGHT : 0.05;

        if (isFalling && nextFeetY <= groundY + 0.05) {
            camera.transform.y = groundY + PLAYER_EYE_HEIGHT;
            verticalVelocity = 0;
            isGrounded = true;
        } else if (isGrounded && isFalling && (currentFeetY - groundY) < snapThreshold) {
            // Força o personagem a "colar" na rampa ao descer (Sticky Feet)
            camera.transform.y = groundY + PLAYER_EYE_HEIGHT;
            verticalVelocity = 0;
            isGrounded = true;
        } else {
            camera.transform.y = nextFeetY + PLAYER_EYE_HEIGHT;
            isGrounded = false;
        }
    }

    /**
     * Apply horizontal movement to the player.
     *
     * @param mx
     * @param mz
     * @param camera
     * @param worldObjects
     */
    private void applyHorizontalMovement(double mx, double mz, Camera camera, List<GameObject> worldObjects) {
        double nextX = camera.transform.x + mx;
        double nextZ = camera.transform.z + mz;

        if (!checkPlayerCollision(nextX, camera.transform.y, nextZ, worldObjects)) {
            camera.transform.x = nextX;
            camera.transform.z = nextZ;
        }
    }

    /**
     * Check the collision between player and the environment.
     * 
     * @param targetX
     * @param targetY
     * @param targetZ
     * @param worldObjects
     * @return
     */
    public boolean checkPlayerCollision(double targetX, double targetY, double targetZ, List<GameObject> worldObjects) {
        double feetY = targetY - PLAYER_EYE_HEIGHT;
        
        // Começamos a verificar colisões de PAREDE um pouco acima dos pés.
        // Isso evita que o próprio chão (como o mezanino) nos trave horizontalmente.
        double collisionStartHeight = feetY + 0.5; 
        double headY = feetY + PLAYER_HEIGHT;

        for (GameObject obj : worldObjects) {
            if (!obj.hasCollision) continue;

            // Lógica para objetos escaláveis (Mesh Collision)
            if (obj.isMeshCollision) {
                double groundH = obj.getWorldHeightAt(targetX, targetZ);

                // Se o centro não está sobre o objeto (groundH == -MAX), verificamos se o TOP
                // da caixa delimitadora é escalável. Isso resolve a subida de degraus (stairs).
                if (groundH == -Double.MAX_VALUE) {
                    double objMaxY = obj.transform.y + (obj.maxY * obj.transform.scaleY);
                    if (objMaxY <= feetY + STEP_HEIGHT) {
                        continue; // Permite entrar no volume para subir
                    }
                }

                // Se houver superfície mas for muito alta para os pés (parede)
                if (groundH == -Double.MAX_VALUE || groundH > feetY + STEP_HEIGHT) {
                    if (engine.checkCollision(targetX, targetZ, collisionStartHeight, headY, PLAYER_RADIUS, obj)) {
                        return true;
                    }
                }
                continue; // É uma parte da rampa/escada que podemos subir
            }

            if (engine.checkCollision(targetX, targetZ, collisionStartHeight, headY, PLAYER_RADIUS, obj))
                return true;
        }
        return false;
    }
}
