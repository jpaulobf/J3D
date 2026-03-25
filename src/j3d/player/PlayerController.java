package j3d.player;

import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.core.IGameWindow;
import j3d.input.InputManager;
import j3d.physics.PhysicsEngine;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

public class PlayerController {

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
    private final double STEP_HEIGHT = 1.3;
    private final double EYE_HEIGHT = PhysicsEngine.PLAYER_EYE_HEIGHT;
    private final double RUN_MULTIPLIER = 2.0;

    public PlayerController(Camera camera, InputManager input, IGameWindow window, PhysicsEngine physics) {
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

        // 4. Apply Horizontal Movement with Step Offset
        applyHorizontalMovement(moveX, 0, worldObjects);
        applyHorizontalMovement(0, moveZ, worldObjects);
    }

    /**
     * Tenta mover o jogador e sobe degraus automaticamente se necessário.
     */
    private void applyHorizontalMovement(double mx, double mz, List<GameObject> worldObjects) {
        double nextX = camera.transform.x + mx;
        double nextZ = camera.transform.z + mz;

        // Se não houver colisão, move normalmente
        if (!physics.checkPlayerCollision(nextX, camera.transform.y, nextZ, worldObjects)) {
            camera.transform.x = nextX;
            camera.transform.z = nextZ;
            return;
        }

        // Se houver colisão, procuramos se algum objeto colidido é um degrau escalável
        double bestStepY = -Double.MAX_VALUE;
        boolean canStepUp = false;

        for (GameObject obj : worldObjects) {
            if (obj.hasCollision && physics.checkPlayerCollision(nextX, camera.transform.y, nextZ, Collections.singletonList(obj))) {
                double objectTop = obj.getWorldMaxY();
                double currentFeetY = camera.transform.y - EYE_HEIGHT;
                double heightDiff = objectTop - currentFeetY;

                // Adicionamos uma pequena margem (0.05) para lidar com erros de precisão (1.0 vs 1.00001)
                if (heightDiff > 0 && heightDiff <= (STEP_HEIGHT + 0.05)) {
                    // Antes de subir, verificamos se o espaço acima do degrau está livre de outros objetos (tetos)
                    if (!physics.checkPlayerCollision(nextX, objectTop + EYE_HEIGHT, nextZ, worldObjects)) {
                        if (objectTop > bestStepY) {
                            bestStepY = objectTop;
                            canStepUp = true;
                        }
                    }
                }
            }
        }

        if (canStepUp) {
            camera.transform.y = bestStepY + EYE_HEIGHT;
            camera.transform.x = nextX;
            camera.transform.z = nextZ;
            verticalVelocity = 0;
            isGrounded = true;
        }
    }
}