package j3d.core;

import j3d.input.InputManager;
import j3d.physics.PhysicsEngine;
import java.awt.event.KeyEvent;
import java.util.List;

public class FirstPersonController {

    private Camera camera;
    private InputManager input;
    private IGameWindow window;
    private PhysicsEngine physics;
    
    // Physics State
    private double verticalVelocity = 0;
    private boolean isGrounded = false;
    private final double GRAVITY = -25.0;
    private final double JUMP_FORCE = 10.0;
    private final double WALK_SPEED = 0.3;
    private final double RUN_MULTIPLIER = 2.0;

    public FirstPersonController(Camera camera, InputManager input, IGameWindow window, PhysicsEngine physics) {
        this.camera = camera;
        this.input = input;
        this.window = window;
        this.physics = physics;
    }

    public void update(double deltaTime, List<GameObject> worldObjects) {
        double speedCorrection = deltaTime * 60.0;

        // 1. Mouse Look
        if (window.isFocused()) {
            int dx = window.getMouseDeltaX(input.getMouseX(), window.getWidth() / 2);
            int dy = window.getMouseDeltaY(input.getMouseY(), window.getHeight() / 2);

            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003;
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
                window.centerMouse();
            }
        }

        // 2. Gravity & Jumping
        verticalVelocity += GRAVITY * deltaTime;

        if (window.isKeyPressedOnce(input, KeyEvent.VK_SPACE) && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // Apply Vertical Movement
        double dy = verticalVelocity * deltaTime;
        double nextY = camera.transform.y + dy;

        if (physics.checkPlayerCollision(camera.transform.x, nextY, camera.transform.z, worldObjects)) {
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

        // 3. Horizontal Movement (WASD)
        double speed = WALK_SPEED * speedCorrection;
        if (window.isKeyDown(java.awt.Toolkit.getDefaultToolkit(), KeyEvent.VK_CAPS_LOCK)) {
            speed *= RUN_MULTIPLIER;
        }

        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);
        double moveX = 0;
        double moveZ = 0;

        if (window.isKeyDown(input, KeyEvent.VK_W)) { moveX += sY * speed; moveZ -= cY * speed; }
        if (window.isKeyDown(input, KeyEvent.VK_S)) { moveX -= sY * speed; moveZ += cY * speed; }
        if (window.isKeyDown(input, KeyEvent.VK_A)) { moveX -= cY * speed; moveZ -= sY * speed; }
        if (window.isKeyDown(input, KeyEvent.VK_D)) { moveX += cY * speed; moveZ += sY * speed; }

        // Apply X
        if (!physics.checkPlayerCollision(camera.transform.x + moveX, camera.transform.y, camera.transform.z, worldObjects)) {
            camera.transform.x += moveX;
        }
        // Apply Z
        if (!physics.checkPlayerCollision(camera.transform.x, camera.transform.y, camera.transform.z + moveZ, worldObjects)) {
            camera.transform.z += moveZ;
        }
    }
}