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
        // 1. Gravity & Jumping
        verticalVelocity += GRAVITY * deltaTime;

        if (jumpRequested && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // Apply Vertical Movement
        double dy = verticalVelocity * deltaTime;
        double nextY = camera.transform.y + dy;

        if (checkPlayerCollision(camera.transform.x, nextY, camera.transform.z, worldObjects)) {
            if (verticalVelocity < 0) {
                isGrounded = true;
                verticalVelocity = 0;
            } else if (verticalVelocity > 0) {
                verticalVelocity = 0;
            }
        } else {
            camera.transform.y = nextY;
            isGrounded = false;
        }

        // 2. Horizontal Movement with Step Offset
        applyHorizontalMovement(moveX, 0, camera, worldObjects);
        applyHorizontalMovement(0, moveZ, camera, worldObjects);
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
            return;
        }

        // Step Offset Logic
        double bestStepY = -Double.MAX_VALUE;
        boolean canStepUp = false;

        for (GameObject obj : worldObjects) {
            if (obj.hasCollision && engine.checkCollision(nextX, nextZ, camera.transform.y - PLAYER_EYE_HEIGHT,
                    camera.transform.y - PLAYER_EYE_HEIGHT + PLAYER_HEIGHT, PLAYER_RADIUS, obj)) {
                double objectTop = obj.getWorldMaxY();
                double currentFeetY = camera.transform.y - PLAYER_EYE_HEIGHT;
                double heightDiff = objectTop - currentFeetY;

                if (heightDiff > 0 && heightDiff <= (STEP_HEIGHT + 0.05)) {
                    if (!checkPlayerCollision(nextX, objectTop + PLAYER_EYE_HEIGHT, nextZ, worldObjects)) {
                        if (objectTop > bestStepY) {
                            bestStepY = objectTop;
                            canStepUp = true;
                        }
                    }
                }
            }
        }

        if (canStepUp) {
            camera.transform.y = bestStepY + PLAYER_EYE_HEIGHT;
            camera.transform.x = nextX;
            camera.transform.z = nextZ;
            verticalVelocity = 0;
            isGrounded = true;
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
        double headY = feetY + PLAYER_HEIGHT;

        for (GameObject obj : worldObjects) {
            if (!obj.hasCollision)
                continue;
            if (engine.checkCollision(targetX, targetZ, feetY, headY, PLAYER_RADIUS, obj))
                return true;
        }
        return false;
    }
}
