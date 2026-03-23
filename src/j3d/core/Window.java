package j3d.core;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Window class responsible for creating the game window, managing the canvas
 * for rendering, and displaying the FPS counter.
 */
public class Window implements IGameWindow {
    private JFrame frame;
    private BufferedImage canvas;
    private int[] canvasPixels;
    private boolean closeRequested = false;

    /**
     * Constructor for the Window class, where we initialize the JFrame, set it to
     * fullscreen, and prepare the canvas for rendering.
     * 
     * @param title
     * @param width
     * @param height
     */
    public Window(String title, int width, int height) {
        frame = new JFrame(title);
        frame.setUndecorated(true); // Removes borders and title bar
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        canvasPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // We don't call super.paintComponent(g) to avoid clearing screen unnecessarily
                Graphics2D g2d = (Graphics2D) g;

                // Hints to prioritize speed when transferring image to screen
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                System.setProperty("sun.java2d.d3d", "true");

                // Scales image from buffer to full window size (Fullscreen)
                g2d.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);

                // Syncs with display hardware to avoid tearing and visual lag
                Toolkit.getDefaultToolkit().sync();
            }
        };
        panel.setPreferredSize(new Dimension(width, height));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximizes the window
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Adapts closing event
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                closeRequested = true;
            }
        });
    }

    /**
     * Updates canvas pixels with renderer data and current FPS, and requests screen
     * repaint.
     * 
     * @param rendererPixels
     */
    @Override
    public void update(int[] rendererPixels) {
        System.arraycopy(rendererPixels, 0, canvasPixels, 0, rendererPixels.length);
        frame.repaint();
    }

    /**
     * Getter for JFrame, necessary to capture input events and get mouse position.
     * 
     * @return
     */
    public JFrame getFrame() {
        return frame;
    }

    @Override
    public boolean shouldClose() {
        return closeRequested;
    }

    @Override
    public void destroy() {
        frame.dispose();
    }
    
    public int getWidth() { return frame.getWidth(); }
    public int getHeight() { return frame.getHeight(); }
}