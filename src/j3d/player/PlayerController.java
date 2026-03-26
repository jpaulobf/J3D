package j3d.player;

import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.input.InputManager;
import j3d.physics.PlayerPhysics;
import j3d.physics.PhysicsEngine;
import j3d.window.IGameWindow;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Player Controller class
 */
public class PlayerController {

    private Camera camera;
    private InputManager input;
    private IGameWindow window;
    private PlayerPhysics playerPhysics;
    private final double WALK_SPEED = 0.3;
    private final double RUN_MULTIPLIER = 2.0;

    /**
     * Constructor for PlayerController.
     * 
     * @param camera
     * @param input
     * @param window
     * @param physics
     */
    public PlayerController(Camera camera, InputManager input, IGameWindow window, PhysicsEngine physics) {
        this.camera = camera;
        this.input = input;
        this.window = window;
        this.playerPhysics = new PlayerPhysics(physics);
    }

    /**
     * Update player commands
     * 
     * @param deltaTime
     * @param worldObjects
     */
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

        // 2. Collect Movement Intent
        boolean jumpRequested = window.isKeyPressedOnce(input, KeyEvent.VK_SPACE);

        // 3. Horizontal Movement (WASD)
        double speed = WALK_SPEED * speedCorrection;
        if (window.isKeyDown(java.awt.Toolkit.getDefaultToolkit(), KeyEvent.VK_CAPS_LOCK)) {
            speed *= RUN_MULTIPLIER;
        }

        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);
        double moveX = 0;
        double moveZ = 0;

        if (window.isKeyDown(input, KeyEvent.VK_W)) {
            moveX += sY * speed;
            moveZ -= cY * speed;
        }
        if (window.isKeyDown(input, KeyEvent.VK_S)) {
            moveX -= sY * speed;
            moveZ += cY * speed;
        }
        if (window.isKeyDown(input, KeyEvent.VK_A)) {
            moveX -= cY * speed;
            moveZ -= sY * speed;
        }
        if (window.isKeyDown(input, KeyEvent.VK_D)) {
            moveX += cY * speed;
            moveZ += sY * speed;
        }

        // 4. Delegate Physics Processing
        playerPhysics.handlePhysics(deltaTime, camera, moveX, moveZ, jumpRequested, worldObjects);
    }
}