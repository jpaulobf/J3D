package j3d.ui;

import j3d.render.IRenderer;

/**
 * Classe responsável por gerenciar e desenhar elementos da interface do usuário
 * (Heads-Up Display).
 */
public class HUD {

    // Textura da mira (crosshair) e controle de visibilidade
    private int[] crosshairPixels;
    private int crosshairSize = 32;
    private boolean visible = true;

    // Fonte simples para números (6x10 pixels)
    private int[][] digitSprites;
    private final int digitW = 6;
    private final int digitH = 10;

    /**
     * Construtor do HUD, que gera a textura da mira proceduralmente.
     */
    public HUD() {
        createCrosshair();
        createDigits();
    }

    /**
     * Gera a textura da mira proceduralmente.
     */
    private void createCrosshair() {
        crosshairPixels = new int[crosshairSize * crosshairSize];
        int color = 0xFF00FF00; // Verde Sólido (Alpha 255)
        int center = crosshairSize / 2;

        for (int y = 0; y < crosshairSize; y++) {
            for (int x = 0; x < crosshairSize; x++) {
                // Lógica simples para desenhar uma cruz com um buraco no meio
                boolean vertical = Math.abs(x - center) < 2 && Math.abs(y - center) > 4;
                boolean horizontal = Math.abs(y - center) < 2 && Math.abs(x - center) > 4;

                if (vertical || horizontal) {
                    crosshairPixels[y * crosshairSize + x] = color;
                }
            }
        }
    }

    /**
     * Gera os sprites para os dígitos 0-9 (Bitmap Font simples).
     */
    private void createDigits() {
        digitSprites = new int[10][digitW * digitH];
        int color = 0xFFFFFF00; // Amarelo Sólido
        int scale = 2;
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
        drawNumber(renderer, fps, 10, 10);
    }

    private void drawNumber(IRenderer renderer, int number, int x, int y) {
        String s = String.valueOf(number);
        for (int i = 0; i < s.length(); i++) {
            int digit = s.charAt(i) - '0';
            renderer.drawSprite(digitSprites[digit], digitW, digitH, x + (i * (digitW + 2)), y);
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
