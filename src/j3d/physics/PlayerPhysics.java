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
        // 1. Jump Logic
        if (jumpRequested && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // 2. Horizontal Movement
        // We process X and Z separately to allow sliding on corners and walls
        if (moveX != 0) {
            applyHorizontalMovement(moveX, 0, camera, worldObjects);
        }
        if (moveZ != 0) {
            applyHorizontalMovement(0, moveZ, camera, worldObjects);
        }

        // 3. Ground and Gravity Logic (Mesh Aware)
        double currentFeetY = camera.transform.y - PLAYER_EYE_HEIGHT;

        // Finds the ground height at the player's exact (X, Z) position
        double groundY = -Double.MAX_VALUE;
        for (GameObject obj : worldObjects) {
            if (!obj.hasCollision)
                continue;

            double h = obj.getWorldHeightAt(camera.transform.x, camera.transform.z);

            // Consider the ground if it's below us or within the Step Height range
            if (h != -Double.MAX_VALUE && h <= (currentFeetY + STEP_HEIGHT + 0.01)) {
                if (h > groundY)
                    groundY = h;
            }
        }

        if (groundY == -Double.MAX_VALUE)
            groundY = 0; // Base floor if nothing is found

        // Apply Gravity
        verticalVelocity += GRAVITY * deltaTime;
        double nextFeetY = currentFeetY + (verticalVelocity * deltaTime);

        // Ground Collision (Floor, Stairs, or Ramp)
        // Increase "Snap" tolerance to avoid the walking-on-air effect when descending
        boolean isFalling = verticalVelocity < 0;
        double snapThreshold = isGrounded ? STEP_HEIGHT : 0.05;

        if (isFalling && nextFeetY <= groundY + 0.05) {
            camera.transform.y = groundY + PLAYER_EYE_HEIGHT;
            verticalVelocity = 0;
            isGrounded = true;
        } else if (isGrounded && isFalling && (currentFeetY - groundY) < snapThreshold) {
            // Forces the character to "stick" to the ramp when descending (Sticky Feet)
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

        // We start checking for WALL collisions slightly above the feet.
        // This prevents the ground itself (like the mezzanine) from locking us
        // horizontally.
        double collisionStartHeight = feetY + 0.5;
        double headY = feetY + PLAYER_HEIGHT;

        for (GameObject obj : worldObjects) {
            if (!obj.hasCollision)
                continue;

            // Logic for scalable objects (Mesh Collision)
            if (obj.isMeshCollision) {
                double groundH = obj.getWorldHeightAt(targetX, targetZ);

                // If the center is not over the object (groundH == -MAX), we check if the TOP
                // of the bounding box is scalable. This resolves climbing stairs.
                if (groundH == -Double.MAX_VALUE) {
                    double objMaxY = obj.transform.y + (obj.maxY * obj.transform.scaleY);
                    if (objMaxY <= feetY + STEP_HEIGHT) {
                        continue; // Allows entering the volume to climb
                    }
                }

                // If there is a surface but it's too high for the feet (wall)
                if (groundH == -Double.MAX_VALUE || groundH > feetY + STEP_HEIGHT) {
                    if (engine.checkCollision(targetX, targetZ, collisionStartHeight, headY, PLAYER_RADIUS, obj)) {
                        return true;
                    }
                }
                continue; // It's part of the ramp/stairs we can climb
            }

            if (engine.checkCollision(targetX, targetZ, collisionStartHeight, headY, PLAYER_RADIUS, obj))
                return true;
        }
        return false;
    }
}
