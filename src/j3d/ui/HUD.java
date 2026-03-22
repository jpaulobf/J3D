package j3d.ui;

import j3d.core.GameObject;
import j3d.render.IRenderer;

/**
 * Class responsible for managing and drawing user interface elements
 * (Heads-Up Display).
 */
public class HUD {

    // Crosshair texture and visibility control
    private int[] crosshairPixels;
    private int crosshairSize;
    private boolean visible = true;

    // Simple font for numbers
    private int[][] digitSprites;
    private int digitW;
    private int digitH;
    private int spacing;
    private int margin;

    // Scanline Icon (Yellow Square)
    private int[] scanlineIconPixels;
    private int scanlineIconSize;

    // SSAA Icon (Green Square)
    private int[] ssaaIconPixels;
    private int ssaaIconSize;

    /**
     * HUD Constructor, which generates the crosshair texture procedurally.
     * 
     * @param width  Screen width for scale calculation
     * @param height Screen height
     */
    public HUD(int width, int height) {
        // Calculates scale based on screen width, using 1920 as reference
        int scale = Math.max(1, Math.round((float) width / 1920.0f));

        this.crosshairSize = 32 * scale;
        this.digitW = 6 * scale; // Base 6 (3x2), proportional scale
        this.digitH = 10 * scale; // Base 10 (5x2), proportional scale
        this.spacing = 2 * scale;
        this.margin = 10 * scale;
        this.scanlineIconSize = 10 * scale; // Base 10x10, escalado
        this.ssaaIconSize = 10 * scale; // Base 10x10, escalado

        createCrosshair(scale);
        createDigits(2 * scale); // Base font is too small (3x5), so multiply by 2*scale
        createScanlineIcon();
        createSSAAIcon();
    }

    /**
     * Procedurally generates the crosshair texture.
     * 
     * @param scale The scaling factor
     */
    private void createCrosshair(int scale) {
        crosshairPixels = new int[crosshairSize * crosshairSize];
        int color = 0xFF00FF00; // Solid Green (Alpha 255)
        int center = crosshairSize / 2;
        int range = 2 * scale;
        int gap = 4 * scale;

        for (int y = 0; y < crosshairSize; y++) {
            for (int x = 0; x < crosshairSize; x++) {
                // Simple logic to draw a cross with a hole in the middle
                boolean vertical = Math.abs(x - center) < range && Math.abs(y - center) > gap;
                boolean horizontal = Math.abs(y - center) < range && Math.abs(x - center) > gap;

                if (vertical || horizontal) {
                    crosshairPixels[y * crosshairSize + x] = color;
                }
            }
        }
    }

    /**
     * Generates sprites for digits 0-9 (Simple Bitmap Font).
     * 
     * @param scale The scaling factor
     */
    private void createDigits(int scale) {
        digitSprites = new int[10][digitW * digitH];
        int color = 0xFFFFFF00; // Solid Yellow
        int originalW = 3;

        // 3x5 Patterns (1 = painted pixel, 0 = transparent)
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
                "111101111001111" // 9
        };

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < patterns[i].length(); j++) {
                if (patterns[i].charAt(j) == '1') {
                    int ox = j % originalW;
                    int oy = j / originalW;

                    // Fills the scaled block (2x2)
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
     * Generates the Scanline icon (Solid yellow square).
     */
    private void createScanlineIcon() {
        scanlineIconPixels = new int[scanlineIconSize * scanlineIconSize];
        int color = 0xFFFFFF00; // Solid Yellow
        for (int i = 0; i < scanlineIconPixels.length; i++) {
            scanlineIconPixels[i] = color;
        }
    }

    /**
     * Generates the SSAA icon (Solid green square).
     */
    private void createSSAAIcon() {
        ssaaIconPixels = new int[ssaaIconSize * ssaaIconSize];
        int color = 0xFF00FF00; // Solid Green
        for (int i = 0; i < ssaaIconPixels.length; i++) {
            ssaaIconPixels[i] = color;
        }
    }

    /**
     * Draws the HUD on the screen using the provided renderer.
     */
    public void draw(IRenderer renderer, int screenWidth, int screenHeight, int fps) {
        if (!visible)
            return;

        // Centers the crosshair on screen
        int x = (screenWidth / 2) - (crosshairSize / 2);
        int y = (screenHeight / 2) - (crosshairSize / 2);

        renderer.drawSprite(crosshairPixels, crosshairSize, crosshairSize, x, y);

        // Draws FPS in the top-left corner (with 10px spacing)
        drawNumber(renderer, fps, margin, margin);

        // Initial position for right-side icons
        int iconX = screenWidth - margin;

        // Draws Scanline indicator in top-right if active
        if (GameObject.scanline) {
            iconX -= scanlineIconSize;
            renderer.drawSprite(scanlineIconPixels, scanlineIconSize, scanlineIconSize, iconX, margin);
        }

        // Draws SSAA indicator next to Scanline
        if (renderer.isSsaaEnabled()) {
            iconX -= (ssaaIconSize + spacing);
            renderer.drawSprite(ssaaIconPixels, ssaaIconSize, ssaaIconSize, iconX, margin);
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
