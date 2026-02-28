package j3d.core;

import j3d.render.IRenderer;
import j3d.render.SoftwareRenderer;
import j3d.lighting.PointLight;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.io.ObjLoader;

import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game implements Runnable {

    // Constantes para a resolução da janela
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    // Variáveis de estado do jogo
    private boolean running = true;
    private boolean wireframe = false;
    private boolean showLightGizmo = true;

    // Componentes do jogo
    private Window window;
    private IRenderer renderer;
    private InputManager input;
    private Robot robot;
    private Camera camera;
    private List<GameObject> objects;
    private List<PointLight> lights;
    private GameObject lightGizmo;

    // Controle de FPS
    private int TARGET_FPS = 120;
    private int fps = 0;
    private int frames = 0;
    private long lastFpsTime = System.currentTimeMillis();
    private static final double PLAYER_RADIUS = 0.5; // Tamanho da colisão do jogador
    private static final double PLAYER_HEIGHT = 1.8; // Altura total do jogador
    private static final double PLAYER_EYE_HEIGHT = 1.6; // Altura dos olhos em relação aos pés

    /**
     * Construtor do jogo, onde inicializamos a janela, o renderer, a câmera, os
     * objetos e as luzes.
     */
    public Game() {
        // Inicialização do jogo
        window = new Window("Engine 3D - J3D Game", WIDTH, HEIGHT);
        input = new InputManager();
        camera = new Camera();
        objects = new ArrayList<>();
        lights = new ArrayList<>();
        renderer = new SoftwareRenderer(WIDTH, HEIGHT);

        // Inicialização do renderer
        renderer.init();

        try {
            robot = new Robot();
        } catch (Exception e) {
        }

        // Esconde o cursor
        window.getFrame().setCursor(window.getFrame().getToolkit().createCustomCursor(
                new BufferedImage(1, 1, 2), new Point(0, 0), ""));

        // Configuração inicial dos objetos da cena
        this.getSceneInitialObjets();

        // Configuração inicial da câmera
        this.initialSceneCameraConfiguration();

        // Configuração dos listeners de input
        window.getFrame().addKeyListener(input);
        window.getFrame().addMouseMotionListener(input);
        window.getFrame().addMouseWheelListener(input);
    }

    /**
     * Configura a posição e orientação inicial da câmera para uma visão adequada da
     * cena.
     */
    private void initialSceneCameraConfiguration() {
        // Configuração inicial da câmera
        camera.transform.z = 15;
        camera.transform.y = 1.5;

        // Orientação validada
        camera.yaw = 2.3;
        camera.pitch = 1.5;
    }

    /**
     * Configura os objetos iniciais da cena, incluindo formas básicas, um modelo 3D
     * importado e uma luz.
     */
    private void getSceneInitialObjets() {
        // Setup da Cena
        GameObject cube = new GameObject(Mesh.createCube());
        cube.transform.x = -3;
        objects.add(cube);

        GameObject pyr = new GameObject(Mesh.createPyramid());
        pyr.transform.x = 3;
        objects.add(pyr);

        GameObject floor = new GameObject(Mesh.createGrid(20, 2.0));
        floor.transform.y = -1.5;
        floor.hasCollision = false; // Desativa colisão com o chão para não travar o movimento
        objects.add(floor);

        // Configuração da luz
        lights.add(new PointLight(0, 5, 0, Color.WHITE, 1.5));
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));

        // leitura do modelo 3D da cena, com textura e cor
        GameObject car = new GameObject(ObjLoader.load("res/Car.obj", Color.CYAN));
        car.transform.y = -1;
        car.transform.x = -6;
        car.transform.z = -5;
        car.transform.setScale(1.5);
        objects.add(car);
    }

    /**
     * Atualiza o estado do jogo, processando o input do usuário para movimentar a
     * câmera e a luz, e atualizando a rotação dos objetos. Também calcula o FPS
     * atual e atualiza o título da janela com essa informação.
     */
    private void update() {
        // Correção de velocidade baseada no FPS para garantir movimento consistente
        double speedCorrection = 60.0 / TARGET_FPS;

        // Toggle de modos de renderização e visualização
        if (input.isKeyPressed(KeyEvent.VK_F2))
            wireframe = !wireframe;
        if (input.isKeyPressed(KeyEvent.VK_F3))
            showLightGizmo = !showLightGizmo;
        if (input.isKeyPressed(KeyEvent.VK_F4))
            GameObject.gouraud = !GameObject.gouraud;

        // Movimento da câmera com mouse
        if (window.getFrame().isFocusOwner()) {
            java.awt.Point loc = window.getFrame().getLocationOnScreen();
            int centerX = loc.x + window.getFrame().getWidth() / 2;
            int centerY = loc.y + window.getFrame().getHeight() / 2;

            int dx = input.getMouseX() - centerX;
            int dy = input.getMouseY() - centerY;

            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003;
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
                robot.mouseMove(centerX, centerY);
            }
        }

        // Movimento vertical da câmera com Scroll
        int scroll = input.getScrollDelta();
        if (scroll != 0) {
            camera.transform.y -= scroll * 0.5; // Invertido: Scroll para trás sobe, para frente desce (ou vice-versa
                                                // conforme preferência)
        }

        // Movimento da câmera com teclado (WASD)
        double camSp = 0.3 * speedCorrection;
        double sY = Math.sin(camera.yaw);
        double cY = Math.cos(camera.yaw);
        
        double dx = 0;
        double dz = 0;

        if (input.isKeyHeld(KeyEvent.VK_W)) {
            dx += sY * camSp;
            dz -= cY * camSp;
        }
        if (input.isKeyHeld(KeyEvent.VK_S)) {
            dx -= sY * camSp;
            dz += cY * camSp;
        }
        if (input.isKeyHeld(KeyEvent.VK_A)) {
            dx -= cY * camSp;
            dz -= sY * camSp;
        }
        if (input.isKeyHeld(KeyEvent.VK_D)) {
            dx += cY * camSp;
            dz += sY * camSp;
        }

        // Aplica movimento no eixo X se não houver colisão
        if (!checkCollision(camera.transform.x + dx, camera.transform.y, camera.transform.z)) {
            camera.transform.x += dx;
        } else {
            // Tenta deslizar (verifica se apenas o movimento em X causou a colisão)
            // Se colidiu, tentamos zerar o DX para ver se conseguimos andar só em Z no próximo bloco?
            // Na verdade, aqui aplicamos X. Se falhar, não aplicamos X.
        }

        // Aplica movimento no eixo Z se não houver colisão (permite deslizar nas paredes)
        if (!checkCollision(camera.transform.x, camera.transform.y, camera.transform.z + dz)) {
            camera.transform.z += dz;
        }

        // Movimento da luz com as setas do teclado
        j3d.lighting.PointLight spot = lights.get(0);
        double lSp = 0.3 * speedCorrection;
        if (input.isKeyHeld(KeyEvent.VK_UP))
            spot.pos.z -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_DOWN))
            spot.pos.z += lSp;
        if (input.isKeyHeld(KeyEvent.VK_LEFT))
            spot.pos.x -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_RIGHT))
            spot.pos.x += lSp;
        if (input.isKeyHeld(KeyEvent.VK_I))
            spot.pos.y += lSp;
        if (input.isKeyHeld(KeyEvent.VK_K))
            spot.pos.y -= lSp;

        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

        // Rotação dos objetos da cena (exceto o chão e o modelo importado)
        int index = 0;
        for (j3d.core.GameObject obj : objects) {
            if (index < 0)
                obj.transform.rotY += 0.03 * speedCorrection;
            index++;
        }

        // Atualização do FPS
        frames++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = System.currentTimeMillis();
            window.getFrame().setTitle("TARGET FPS: " + TARGET_FPS + " | ACTUAL FPS: " + fps);
        }
    }

    /**
     * Verifica colisão da câmera contra todos os objetos da cena
     */
    private boolean checkCollision(double targetX, double targetY, double targetZ) {
        double feetY = targetY - PLAYER_EYE_HEIGHT;
        double headY = feetY + PLAYER_HEIGHT;

        for (GameObject obj : objects) {
            if (obj.checkCollision(targetX, targetZ, feetY, headY, PLAYER_RADIUS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / TARGET_FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (running) {
            update();
            renderer.clear();

            renderer.draw(camera, objects, lights, wireframe);
            if (showLightGizmo) {
                renderer.draw(camera, Arrays.asList(lightGizmo), null, true);
            }

            window.update(renderer.getFrameBuffer(), fps);

            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime /= 1000000;

                if (remainingTime < 0) {
                    remainingTime = 0;
                }

                Thread.sleep((long) remainingTime);

                nextDrawTime += drawInterval;
            } catch (Exception e) {
            }
        }
    }
}