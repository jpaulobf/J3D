package j3d.core;

import j3d.render.IRenderer;
import j3d.render.SoftwareRenderer;
import j3d.lighting.PointLight;
import j3d.physics.PhysicsEngine;
import j3d.geometry.Mesh;
import j3d.input.InputManager;
import j3d.io.ObjLoader;

import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import j3d.ui.HUD;

/**
 * Game class responsible for initializing the game, handling the main game
 * loop, processing user input, updating the game state, and rendering the
 * scene.
 */
public class Game implements Runnable {

    // Constantes para a resolução da janela
    private static final int WIDTH = 1366;
    private static final int HEIGHT = 768;

    // Variáveis de estado do jogo
    private boolean running = true;
    private boolean wireframe = false;
    private boolean showLightGizmo = false;

    // Componentes do jogo
    private Window window;
    private IRenderer renderer;
    private InputManager input;
    private Robot robot;
    private Camera camera;
    private List<GameObject> objects;
    private List<PointLight> lights;
    private GameObject lightGizmo;
    private List<GameObject> gizmoList;
    private PhysicsEngine physics;

    // UI / HUD
    private HUD hud;

    // Controle de FPS
    private int TARGET_FPS = 120;
    private int fps = 0;
    private int frames = 0;
    private long lastFpsTime = System.currentTimeMillis();
    private int windowCenterX = WIDTH / 2;
    private int windowCenterY = HEIGHT / 2;

    // Física do Jogador
    private double verticalVelocity = 0;
    private boolean isGrounded = false;
    private static final double GRAVITY = -25.0; // Aceleração da gravidade
    private static final double JUMP_FORCE = 10.0; // Força do pulo

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
        gizmoList = new ArrayList<>();
        renderer = new SoftwareRenderer(WIDTH, HEIGHT);
        physics = new PhysicsEngine();
        hud = new HUD(WIDTH, HEIGHT);

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

        // Garante que a janela receba o foco do teclado imediatamente ao iniciar
        window.getFrame().requestFocus();
    }

    /**
     * Configura a posição e orientação inicial da câmera para uma visão adequada da
     * cena.
     */
    private void initialSceneCameraConfiguration() {
        // Configuração inicial da câmera
        // Posiciona o jogador no CENTRO da primeira sala (Sala Esquerda)
        // Indices (1.5, 2.0) * blockSize (10.0)
        camera.transform.x = 15.0;
        camera.transform.z = 20.0;
        camera.transform.y = 7.5; // Altura dos olhos ajustada para o novo pé direito

        // Orientação validada
        camera.yaw = 0;
        camera.pitch = 0;
    }

    /**
     * Helper para criar blocos sólidos (Paredes, Pisos, Degraus).
     * Usa o cubo padrão e ajusta a escala e posição.
     */
    private void createBlock(double x, double y, double z, double sX, double sY, double sZ, Color color) {
        // Cria o cubo base
        Mesh m = Mesh.createCube();
        
        // Opcional: Pintar o cubo de uma cor sólida para ficar mais bonito que o arco-íris padrão
        if (color != null) {
            for (j3d.geometry.Triangle t : m.triangles) {
                t.baseColor = color;
            }
        }

        GameObject obj = new GameObject(m);
        obj.transform.x = x;
        obj.transform.y = y;
        obj.transform.z = z;
        obj.transform.scaleX = sX;
        obj.transform.scaleY = sY;
        obj.transform.scaleZ = sZ;
        
        objects.add(obj);
    }

    /**
     * Configura os objetos iniciais da cena, incluindo formas básicas, um modelo 3D
     * importado e uma luz.
     */
    private void getSceneInitialObjets() {
        // Cores para o cenário
        Color floorColor = new Color(50, 50, 50);
        Color wallColor = new Color(100, 100, 120);
        Color platformColor = new Color(150, 100, 50); // Madeira escura
        Color stairColor = new Color(180, 120, 60);    // Madeira clara
        Color pillarColor = new Color(80, 80, 80);

        // 1. CHÃO PRINCIPAL (Nível 0)
        // Cria um chão grande (Scale X=50, Z=50)
        // Nota: O cubo vai de -1 a 1, então scale 50 gera um tamanho total de 100.
        createBlock(0, -1.0, 0, 50, 1, 50, floorColor);

        // 2. PAREDES EXTERNAS (Arena)
        // Paredes altas ao redor
        createBlock(-50, 10, 0, 1, 20, 50, wallColor); // Parede Esquerda
        createBlock(50, 10, 0, 1, 20, 50, wallColor);  // Parede Direita
        createBlock(0, 10, 50, 50, 20, 1, wallColor);  // Parede Fundo
        createBlock(0, 10, -50, 50, 20, 1, wallColor); // Parede Frente

        // 3. MEZANINO (Segundo Andar)
        // Uma plataforma elevada em Y=10, ocupando o fundo da sala
        double mezaninoY = 10.0;
        createBlock(0, mezaninoY, 30, 30, 1, 15, platformColor);

        // Pilares de sustentação do mezanino (Visual)
        createBlock(-25, 0, 40, 2, mezaninoY, 2, pillarColor);
        createBlock(25, 0, 40, 2, mezaninoY, 2, pillarColor);
        createBlock(-25, 0, 20, 2, mezaninoY, 2, pillarColor);
        createBlock(25, 0, 20, 2, mezaninoY, 2, pillarColor);

        // 4. ESCADARIA CENTRAL
        // Cria degraus subindo do chão até o mezanino
        double startZ = 10.0; // Começa um pouco antes do mezanino
        double startY = 0.0;
        int steps = 10;
        double stepHeight = mezaninoY / steps; // 1.0 de altura por degrau
        double stepDepth = 2.0;
        double stepWidth = 8.0;

        for (int i = 0; i < steps; i++) {
            // Cada degrau sobe em Y e avança em Z
            double y = startY + (i * stepHeight);
            double z = startZ + (i * stepDepth);
            
            // O Y do createBlock é o centro do objeto. 
            // Se o degrau tem altura 'stepHeight', o centro deve ser ajustado.
            createBlock(0, y, z, stepWidth, stepHeight, stepDepth, stairColor);
        }

        // 5. PASSARELA SUPERIOR (Ponte)
        // Conecta o mezanino a uma varanda lateral
        createBlock(-35, mezaninoY, 10, 5, 1, 20, platformColor); // Ponte lateral esquerda
        createBlock(-45, mezaninoY, -10, 5, 1, 5, platformColor); // Pequena varanda

        // leitura do modelo 3D da cena, com textura e cor
        // Usa loadScene para separar os móveis/paredes em objetos com colisões individuais
        List<GameObject> sceneObjects = ObjLoader.loadScene("res/sala.obj", Color.LIGHT_GRAY);
        for (GameObject obj : sceneObjects) {
            obj.transform.y = -0.5;
            obj.transform.x = -6;
            obj.transform.z = -5;
            obj.transform.setScale(20);
            objects.add(obj);
        }

        // Configuração da luz
        lights.add(new PointLight(0, 0, 0, Color.WHITE, 2)); // Luz "Lanterna"
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));
        gizmoList.add(lightGizmo);
    }

    /**
     * Atualiza o estado do jogo, processando o input do usuário para movimentar a
     * câmera e a luz, e atualizando a rotação dos objetos. Também calcula o FPS
     * atual e atualiza o título da janela com essa informação.
     */
    private void update(double deltaTime) {
        // Correção de velocidade baseada no FPS para garantir movimento consistente
        double speedCorrection = deltaTime * 60.0;

        // Toggle de modos de renderização e visualização
        if (input.isKeyPressed(KeyEvent.VK_F2))
            wireframe = !wireframe;
        if (input.isKeyPressed(KeyEvent.VK_F3))
            showLightGizmo = !showLightGizmo;
        if (input.isKeyPressed(KeyEvent.VK_F4))
            GameObject.gouraud = !GameObject.gouraud;

        if (input.isKeyPressed(KeyEvent.VK_F5)) {
            // Supondo que você faça um cast se a sua variável renderer for uma interface
            if (renderer instanceof SoftwareRenderer) {
                SoftwareRenderer sr = (SoftwareRenderer) renderer;
                sr.ssaaEnabled = !sr.ssaaEnabled;
                System.out.println("SSAA 2x: " + (sr.ssaaEnabled ? "LIGADO" : "DESLIGADO"));
            }
        }

        if (input.isKeyPressed(KeyEvent.VK_F6))
            hud.setVisible(!hud.isVisible());

        if (input.isKeyPressed(KeyEvent.VK_F10)) {
            GameObject.scanline = !GameObject.scanline;
            System.out.println("Rasterização Scanline: " + (GameObject.scanline ? "LIGADO" : "DESLIGADO"));
        }

        if (input.isKeyPressed(KeyEvent.VK_ESCAPE))
            System.exit(0);

        // Movimento da câmera com mouse
        if (window.getFrame().isFocusOwner()) {
            
            // Calcula o deslocamento do mouse a partir do centro da janela
            int dx = input.getMouseX() - windowCenterX;
            int dy = input.getMouseY() - windowCenterY;

            if (dx != 0 || dy != 0) {
                camera.yaw += dx * 0.003;
                camera.pitch += dy * 0.003;
                camera.pitch = Math.max(-1.5, Math.min(1.5, camera.pitch));
                robot.mouseMove(windowCenterX, windowCenterY);
            }
        }

        // --- FÍSICA VERTICAL (Gravidade e Pulo) ---
        
        // Aplica gravidade na velocidade vertical
        verticalVelocity += GRAVITY * deltaTime;

        // Pulo (apenas se estiver no chão)
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && isGrounded) {
            verticalVelocity = JUMP_FORCE;
            isGrounded = false;
        }

        // Aplica velocidade vertical e resolve colisão
        double dy = verticalVelocity * deltaTime;
        double nextY = camera.transform.y + dy;

        if (physics.checkPlayerCollision(camera.transform.x, nextY, camera.transform.z, objects)) {
            if (verticalVelocity < 0) { 
                isGrounded = true; // Colidiu caindo -> Chão
                verticalVelocity = 0;
            } else if (verticalVelocity > 0) {
                verticalVelocity = 0; // Colidiu subindo -> Teto
            }
            // Se colidiu, não atualiza o Y (impede atravessar)
        } else {
            camera.transform.y = nextY;
            isGrounded = false; // Se moveu livremente no Y, está no ar (ou caindo)
        }

        // Movimento da câmera com teclado (WASD)
        double baseSpeed = 0.3;
        try {
            // Dobra a velocidade se Caps Lock estiver ativado (modo "sprint")
            if (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) {
                baseSpeed *= 2; // Dobra a velocidade
            }
        } catch (Exception e) { }

        double camSp = baseSpeed * speedCorrection;
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
        if (!physics.checkPlayerCollision(camera.transform.x + dx, camera.transform.y, camera.transform.z, objects)) {
            camera.transform.x += dx;
        }

        // Aplica movimento no eixo Z se não houver colisão (permite deslizar nas
        // paredes)
        if (!physics.checkPlayerCollision(camera.transform.x, camera.transform.y, camera.transform.z + dz, objects)) {
            camera.transform.z += dz;
        }

        // Lógica da Luz (Lanterna)
        // A luz segue a posição da câmera, mas um pouco à frente
        j3d.lighting.PointLight spot = lights.get(0);
        spot.pos.x = camera.transform.x;
        spot.pos.y = camera.transform.y;
        spot.pos.z = camera.transform.z;

        // Controles manuais da luz (opcional, mantido para debug)
        double lSp = 0.3 * speedCorrection;
        if (input.isKeyHeld(KeyEvent.VK_U))
            spot.pos.z -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_O))
            spot.pos.z += lSp;
        if (input.isKeyHeld(KeyEvent.VK_J))
            spot.pos.x -= lSp;
        if (input.isKeyHeld(KeyEvent.VK_L))
            spot.pos.x += lSp;
        if (input.isKeyHeld(KeyEvent.VK_I))
            spot.pos.y += lSp;
        if (input.isKeyHeld(KeyEvent.VK_K))
            spot.pos.y -= lSp;

        lightGizmo.transform.x = spot.pos.x;
        lightGizmo.transform.y = spot.pos.y;
        lightGizmo.transform.z = spot.pos.z;

        // Atualização do FPS
        frames++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fps = frames;
            frames = 0;
            lastFpsTime = System.currentTimeMillis();

            // Atualiza a posição da janela apenas uma vez por segundo (se o usuário moveu a
            // janela)
            try {
                Point loc = window.getFrame().getLocationOnScreen();
                windowCenterX = loc.x + window.getFrame().getWidth() / 2;
                windowCenterY = loc.y + window.getFrame().getHeight() / 2;
            } catch (Exception e) {
            }

            // window.getFrame().setTitle("TARGET FPS: " + TARGET_FPS + " | ACTUAL FPS: " +
            // fps);
        }
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / TARGET_FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;
        long lastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            // Calcula o tempo decorrido desde o último frame em segundos
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            update(deltaTime);
            renderer.clear();

            renderer.draw(camera, objects, lights, wireframe);
            if (showLightGizmo) {
                renderer.draw(camera, gizmoList, null, true);
            }

            hud.draw(renderer, WIDTH, HEIGHT, fps);

            window.update(renderer.getFrameBuffer());

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