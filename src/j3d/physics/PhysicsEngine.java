package j3d.physics;

import j3d.core.GameObject;

/**
 * PhysicsEngine class responsible for handling collision detection and
 * physics-related calculations in the game.
 */
public class PhysicsEngine {

    /**
     * Checks if a position (usually the camera) collides with this object.
     * The check is done using AABB (Axis-Aligned Bounding Box).
     * 
     * @param x          Entity X position
     * @param z          Entity Z position
     * @param entityMinY Entity base Y position (feet)
     * @param entityMaxY Entity top Y position (head)
     * @param radius     Entity radius (used to define player box)
     * @return true if there is a collision
     */
    public boolean checkCollision(double x, double z, double entityMinY, double entityMaxY, double radius,
            GameObject object) {
        if (!object.hasCollision)
            return false;

        // 1. Define player AABB (entity)
        double playerMinX = x - radius;
        double playerMaxX = x + radius;
        double playerMinZ = z - radius;
        double playerMaxZ = z + radius;

        // 2. Define object AABB in world space
        double objMinX = object.transform.x + object.minX * object.transform.scaleX;
        double objMaxX = object.transform.x + object.maxX * object.transform.scaleX;
        double objMinY = object.transform.y + object.minY * object.transform.scaleY;
        double objMaxY = object.transform.y + object.maxY * object.transform.scaleY;
        double objMinZ = object.transform.z + object.minZ * object.transform.scaleZ;
        double objMaxZ = object.transform.z + object.maxZ * object.transform.scaleZ;

        // 3. Check overlap on all 3 axes. Collision exists if all overlap.
        return (playerMinX < objMaxX && playerMaxX > objMinX) &&
                (entityMinY < objMaxY && entityMaxY > objMinY) &&
                (playerMinZ < objMaxZ && playerMaxZ > objMinZ);
    }

    /**
     * Checks AABB (Axis-Aligned Bounding Box) collision between two GameObjects.
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
