package j3d.render;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import j3d.core.Camera;
import j3d.core.GameObject;
import j3d.lighting.PointLight;

/**
 * SoftwareRenderer class implementing the IRenderer interface, responsible for
 * rendering a 3D scene using software rendering techniques, including handling
 * frame buffer management and z-buffering for depth handling.
 */
public class SoftwareRenderer implements IRenderer {

    // Dimensions of the rendering area, frame buffer for pixel colors, and z-buffer
    // for depth values
    private final int width, height;
    private int[] pixels;
    private double[] zBuffer;

    // High Resolution Buffers (2x) for SSAA
    private int[] hrPixels;
    private double[] hrZBuffer;

    public boolean ssaaEnabled = false;

    /**
     * Constructor for SoftwareRenderer.
     * 
     * @param width
     * @param height
     */
    public SoftwareRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Initializes the renderer, setting up necessary resources and configurations.
     */
    @Override
    public void init() {
        this.pixels = new int[width * height];
        this.zBuffer = new double[width * height];

        // Initializes giant buffers (4 times more pixels in total!)
        hrPixels = new int[(width * 2) * (height * 2)];
        hrZBuffer = new double[(width * 2) * (height * 2)];
    }

    /**
     * Clears the frame buffer, preparing for a new frame to be drawn.
     */
    @Override
    public void clear() {
        if (ssaaEnabled) {
            int h = height * 2;
            int w = width * 2;
            for (int y = 0; y < h; y++) {
                // Gradiente vertical: Azul Profundo (Topo) -> Azul Claro (Baixo)
                int color = interpolateColor(0xFF1E90FF, 0xFF87CEEB, (float) y / h);
                Arrays.fill(hrPixels, y * w, y * w + w, color);
            }
            Arrays.fill(hrZBuffer, 0.0);
        } else {
            for (int y = 0; y < height; y++) {
                int color = interpolateColor(0xFF1E90FF, 0xFF87CEEB, (float) y / height);
                Arrays.fill(pixels, y * width, y * width + width, color);
            }
            Arrays.fill(zBuffer, 0.0);
        }
    }

    /**
     * Generic draw method that doesn't know scene logic.
     */
    @Override
    public void draw(Camera cam, List<GameObject> objects, List<PointLight> lights, boolean wireframe) {
        if (objects == null)
            return;

        for (GameObject obj : objects) {
            if (ssaaEnabled) {
                // Sends GameObject to draw on giant buffer (2x)
                obj.draw(hrPixels, hrZBuffer, cam, lights, width * 2, height * 2, wireframe);
            } else {
                // Normal drawing (1x)
                obj.draw(pixels, zBuffer, cam, lights, width, height, wireframe);
            }
        }
    }

    /**
     * Draws a 2D sprite over the scene.
     * Supports transparency (Alpha Channel) and scales for SSAA.
     */
    @Override
    public void drawSprite(int[] spritePixels, int spriteW, int spriteH, int x, int y) {
        if (ssaaEnabled) {
            drawSpriteSSAA(spritePixels, spriteW, spriteH, x, y);
        } else {
            drawSpriteNormal(spritePixels, spriteW, spriteH, x, y);
        }
    }

    /**
     * Draws a 2D sprite directly into pixel buffer, ignoring Z-Buffer.
     * Useful for HUD, crosshairs, and interfaces.
     * 
     * @param spritePixels
     * @param spriteW
     * @param spriteH
     * @param x
     * @param y
     */
    private void drawSpriteNormal(int[] spritePixels, int spriteW, int spriteH, int x, int y) {
        for (int j = 0; j < spriteH; j++) {
            int py = y + j;
            if (py < 0 || py >= height)
                continue;

            int rowOffset = py * width;
            int spriteRowOffset = j * spriteW;

            for (int i = 0; i < spriteW; i++) {
                int px = x + i;
                if (px < 0 || px >= width)
                    continue;

                int color = spritePixels[spriteRowOffset + i];
                // Checks Alpha channel (ARGB). If 0, it is fully transparent.
                if ((color >>> 24) != 0) {
                    pixels[rowOffset + px] = color;
                }
            }
        }
    }

    /**
     * Draws a 2D sprite directly into high-resolution pixel buffer, ignoring
     * Z-Buffer.
     * Useful for HUD, crosshairs, and interfaces, maintaining sprite visual size
     * even with SSAA.
     * 
     * @param spritePixels
     * @param spriteW
     * @param spriteH
     * @param x
     * @param y
     */
    private void drawSpriteSSAA(int[] spritePixels, int spriteW, int spriteH, int x, int y) {
        int hrW = width * 2;
        int hrH = height * 2;

        // Scales position for 2x buffer
        int startX = x * 2;
        int startY = y * 2;

        for (int j = 0; j < spriteH; j++) {
            int py = startY + j * 2;
            if (py >= hrH)
                break; // Passed bottom limit

            int spriteRowOffset = j * spriteW;

            for (int i = 0; i < spriteW; i++) {
                int px = startX + i * 2;
                if (px >= hrW)
                    break; // Passed right limit

                int color = spritePixels[spriteRowOffset + i];

                if ((color >>> 24) != 0) {
                    // Draws a 2x2 block on high-res buffer
                    // to maintain sprite visual size on screen
                    if (py >= 0 && px >= 0)
                        hrPixels[py * hrW + px] = color;
                    if (py >= 0 && px + 1 < hrW && px + 1 >= 0)
                        hrPixels[py * hrW + (px + 1)] = color;
                    if (py + 1 < hrH && py + 1 >= 0 && px >= 0)
                        hrPixels[(py + 1) * hrW + px] = color;
                    if (py + 1 < hrH && py + 1 >= 0 && px + 1 < hrW && px + 1 >= 0)
                        hrPixels[(py + 1) * hrW + (px + 1)] = color;
                }
            }
        }
    }

    /**
     * Resolves the SSAA by averaging the colors of the 4 corresponding pixels in
     * the high-resolution buffer to produce the final color for each pixel in the
     * normal frame buffer.
     */
    private void resolveSSAA() {
        int hrWidth = width * 2;

        // Parallel processing to utilize all CPU cores
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowOffset = y * width;
            int hrRow1Offset = (y * 2) * hrWidth;
            int hrRow2Offset = hrRow1Offset + hrWidth;

            for (int x = 0; x < width; x++) {
                int hrX = x * 2;

                int p1 = hrPixels[hrRow1Offset + hrX];
                int p2 = hrPixels[hrRow1Offset + hrX + 1];
                int p3 = hrPixels[hrRow2Offset + hrX];
                int p4 = hrPixels[hrRow2Offset + hrX + 1];

                // Bitwise Optimization: Sums masked channels and divides by 4 (>> 2) at the end
                int b = (p1 & 0xFF) + (p2 & 0xFF) + (p3 & 0xFF) + (p4 & 0xFF);
                int g = (p1 & 0xFF00) + (p2 & 0xFF00) + (p3 & 0xFF00) + (p4 & 0xFF00);
                int r = (p1 & 0xFF0000) + (p2 & 0xFF0000) + (p3 & 0xFF0000) + (p4 & 0xFF0000);

                pixels[rowOffset + x] = ((r >> 2) & 0xFF0000) | ((g >> 2) & 0xFF00) | (b >> 2);
            }
        });
    }

    /**
     * Returns the current frame buffer as an array of integers representing pixel
     * colors.
     * 
     * @return
     */
    @Override
    public int[] getFrameBuffer() {
        if (ssaaEnabled) {
            resolveSSAA(); // Downsamples the giant image before sending to screen
        }
        return pixels;
    }

    @Override
    public boolean isSsaaEnabled() {
        return ssaaEnabled;
    }

    /**
     * Interpolates between two colors based on a ratio t (0.0 to 1.0).
     */
    private int interpolateColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void toggleSsaa() {
        this.ssaaEnabled = !this.ssaaEnabled;
    }
}