package j3d.ui;

import j3d.core.GameObject;
import j3d.render.IRenderer;

/**
 * Classe responsável por gerenciar e desenhar elementos da interface do usuário
 * (Heads-Up Display).
 */
public class HUD {

    // Textura da mira (crosshair) e controle de visibilidade
    private int[] crosshairPixels;
    private int crosshairSize;
    private boolean visible = true;

    // Fonte simples para números
    private int[][] digitSprites;
    private int digitW;
    private int digitH;
    private int spacing;
    private int margin;

    // Ícone de Scanline (Quadrado Amarelo)
    private int[] scanlineIconPixels;
    private int scanlineIconSize;

    /**
     * Construtor do HUD, que gera a textura da mira proceduralmente.
     * @param width Largura da tela para cálculo de escala
     * @param height Altura da tela
     */
    public HUD(int width, int height) {
        // Calcula a escala com base na largura da tela, usando 1920 como referência
        int scale = Math.max(1, Math.round((float) width / 1920.0f));

        this.crosshairSize = 32 * scale;
        this.digitW = 6 * scale;   // Base 6 (3x2), escala proporcional
        this.digitH = 10 * scale;  // Base 10 (5x2), escala proporcional
        this.spacing = 2 * scale;
        this.margin = 10 * scale;
        this.scanlineIconSize = 10 * scale; // Base 10x10, escalado

        createCrosshair(scale);
        createDigits(2 * scale); // A fonte base é muito pequena (3x5), então multiplicamos por 2*scale
        createScanlineIcon();
    }

    /**
     * Gera a textura da mira proceduralmente.
     */
    private void createCrosshair(int scale) {
        crosshairPixels = new int[crosshairSize * crosshairSize];
        int color = 0xFF00FF00; // Verde Sólido (Alpha 255)
        int center = crosshairSize / 2;
        int range = 2 * scale;
        int gap = 4 * scale;

        for (int y = 0; y < crosshairSize; y++) {
            for (int x = 0; x < crosshairSize; x++) {
                // Lógica simples para desenhar uma cruz com um buraco no meio
                boolean vertical = Math.abs(x - center) < range && Math.abs(y - center) > gap;
                boolean horizontal = Math.abs(y - center) < range && Math.abs(x - center) > gap;

                if (vertical || horizontal) {
                    crosshairPixels[y * crosshairSize + x] = color;
                }
            }
        }
    }

    /**
     * Gera os sprites para os dígitos 0-9 (Bitmap Font simples).
     */
    private void createDigits(int scale) {
        digitSprites = new int[10][digitW * digitH];
        int color = 0xFFFFFF00; // Amarelo Sólido
        int originalW = 3;

        // Padrões 3x5 (1 = pixel pintado, 0 = transparente)
        String[] patterns = {
            "111101101101111", // 0
            "010010010010010", // 1
            "111001111100111", // 2
            "111001111001111", // 3
            "101101111001001", // 4
            "111100111001111", // 5
            "111100111101111", // 6
            "111001001001001", // 7
            "111101111101111", // 8
            "111101111001111"  // 9
        };

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < patterns[i].length(); j++) {
                if (patterns[i].charAt(j) == '1') {
                    int ox = j % originalW;
                    int oy = j / originalW;

                    // Preenche o bloco escalado (2x2)
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            int px = ox * scale + dx;
                            int py = oy * scale + dy;
                            digitSprites[i][py * digitW + px] = color;
                        }
                    }
                }
            }
        }
    }

    /**
     * Gera o ícone de Scanline (Quadrado amarelo sólido).
     */
    private void createScanlineIcon() {
        scanlineIconPixels = new int[scanlineIconSize * scanlineIconSize];
        int color = 0xFFFFFF00; // Amarelo Sólido
        for (int i = 0; i < scanlineIconPixels.length; i++) {
            scanlineIconPixels[i] = color;
        }
    }

    /**
     * Desenha o HUD na tela usando o renderer fornecido.
     */
    public void draw(IRenderer renderer, int screenWidth, int screenHeight, int fps) {
        if (!visible)
            return;

        // Centraliza a mira na tela
        int x = (screenWidth / 2) - (crosshairSize / 2);
        int y = (screenHeight / 2) - (crosshairSize / 2);

        renderer.drawSprite(crosshairPixels, crosshairSize, crosshairSize, x, y);

        // Desenha o FPS no canto superior esquerdo (com espaçamento de 10px)
        drawNumber(renderer, fps, margin, margin);

        // Desenha o indicador de Scanline no canto superior direito se estiver ativo
        if (GameObject.scanline) {
            int iconX = screenWidth - margin - scanlineIconSize;
            int iconY = margin;
            renderer.drawSprite(scanlineIconPixels, scanlineIconSize, scanlineIconSize, iconX, iconY);
        }
    }

    private void drawNumber(IRenderer renderer, int number, int x, int y) {
        String s = String.valueOf(number);
        for (int i = 0; i < s.length(); i++) {
            int digit = s.charAt(i) - '0';
            renderer.drawSprite(digitSprites[digit], digitW, digitH, x + (i * (digitW + spacing)), y);
        }
    }

    // --- Getters e Setters ---
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }
}
