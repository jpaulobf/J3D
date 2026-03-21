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
import java.util.HashMap;
import java.util.Map;
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
        GameObject cube = new GameObject(ObjLoader.load("res/cube.obj", Color.RED));
        cube.transform.y = -0.5;
        cube.transform.x = -6;
        cube.transform.z = -5;
        cube.transform.setScale(20);
        objects.add(cube);

        // Configuração da luz
        lights.add(new PointLight(0, 0, 0, Color.WHITE, 2)); // Luz "Lanterna"
        lightGizmo = new GameObject(Mesh.createSphere(0.2, 8, 8));
        gizmoList.add(lightGizmo);
    }

    /**
     * Cria uma malha customizada para um único bloco do mapa, contendo apenas as
     * faces visíveis (não adjacentes a outros blocos).
     */
    private Mesh createSingleBlockMesh(int[][] map, int x, int z, double blockSize, double wallHeight) {
        List<j3d.geometry.Vertex> vertices = new ArrayList<>();
        List<j3d.geometry.Triangle> triangles = new ArrayList<>();
        Map<String, Integer> vertexMap = new HashMap<>();

        // Função auxiliar para adicionar vértices sem duplicação (vertex welding)
        java.util.function.Function<j3d.geometry.Vertex, Integer> addVertex = (v) -> {
            String key = String.format("%.2f_%.2f_%.2f", v.x, v.y, v.z);
            if (vertexMap.containsKey(key)) {
                return vertexMap.get(key);
            } else {
                int index = vertices.size();
                vertices.add(v);
                vertexMap.put(key, index);
                return index;
            }
        };

        Color wallColor = Color.GRAY;

        // Verifica os vizinhos para determinar quais faces desenhar
        boolean frontIsEmpty = z + 1 >= map.length || map[z + 1][x] == 0;
        boolean backIsEmpty = z - 1 < 0 || map[z - 1][x] == 0;
        boolean rightIsEmpty = x + 1 >= map[0].length || map[z][x + 1] == 0;
        boolean leftIsEmpty = x - 1 < 0 || map[z][x - 1] == 0;
        boolean topIsEmpty = true; // Topo da parede é sempre visível
        boolean bottomIsEmpty = true; // Base da parede é sempre visível (contra o chão)

        // Coordenadas do mundo para os 8 cantos deste bloco
        double x0 = x * blockSize;
        double x1 = (x + 1) * blockSize;
        double z0 = z * blockSize;
        double z1 = (z + 1) * blockSize;
        double y0 = 0;
        double y1 = wallHeight;

        j3d.geometry.Vertex v000 = new j3d.geometry.Vertex(x0, y0, z0);
        j3d.geometry.Vertex v100 = new j3d.geometry.Vertex(x1, y0, z0);
        j3d.geometry.Vertex v010 = new j3d.geometry.Vertex(x0, y1, z0);
        j3d.geometry.Vertex v110 = new j3d.geometry.Vertex(x1, y1, z0);
        j3d.geometry.Vertex v001 = new j3d.geometry.Vertex(x0, y0, z1);
        j3d.geometry.Vertex v101 = new j3d.geometry.Vertex(x1, y0, z1);
        j3d.geometry.Vertex v011 = new j3d.geometry.Vertex(x0, y1, z1);
        j3d.geometry.Vertex v111 = new j3d.geometry.Vertex(x1, y1, z1);

        // Gera os triângulos (quads) para cada face visível
        if (rightIsEmpty) { // Face Direita (+X)
            int i1 = addVertex.apply(v100), i2 = addVertex.apply(v110), i3 = addVertex.apply(v111), i4 = addVertex.apply(v101);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }
        if (leftIsEmpty) { // Face Esquerda (-X)
            int i1 = addVertex.apply(v001), i2 = addVertex.apply(v011), i3 = addVertex.apply(v010), i4 = addVertex.apply(v000);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }
        if (frontIsEmpty) { // Face Frontal (+Z)
            int i1 = addVertex.apply(v001), i2 = addVertex.apply(v101), i3 = addVertex.apply(v111), i4 = addVertex.apply(v011);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }
        if (backIsEmpty) { // Face Traseira (-Z)
            int i1 = addVertex.apply(v100), i2 = addVertex.apply(v000), i3 = addVertex.apply(v010), i4 = addVertex.apply(v110);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }
        if (topIsEmpty) { // Face Superior (+Y)
            int i1 = addVertex.apply(v010), i2 = addVertex.apply(v011), i3 = addVertex.apply(v111), i4 = addVertex.apply(v110);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }
        if (bottomIsEmpty) { // Face Inferior (-Y)
            int i1 = addVertex.apply(v001), i2 = addVertex.apply(v000), i3 = addVertex.apply(v100), i4 = addVertex.apply(v101);
            triangles.add(new j3d.geometry.Triangle(i1, i2, i3, wallColor)); triangles.add(new j3d.geometry.Triangle(i1, i3, i4, wallColor));
        }

        return new Mesh(vertices, triangles);
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

        // Movimento vertical da câmera com Scroll
        int scroll = input.getScrollDelta();
        if (scroll != 0) {
            camera.transform.y -= scroll * 0.5; // Invertido: Scroll para trás sobe, para frente desce (ou vice-versa
                                                // conforme preferência)
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