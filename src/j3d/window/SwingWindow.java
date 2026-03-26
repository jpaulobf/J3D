package j3d.window;

import javax.swing.*;
import j3d.input.InputManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Window class responsible for creating the game window, managing the canvas
 * for rendering, and displaying the FPS counter.
 */
public class SwingWindow implements IGameWindow {
    private JFrame frame;
    private BufferedImage canvas;
    private int[] canvasPixels;
    private boolean closeRequested = false;
    private int originalWidth;
    private int originalHeight;
    private Robot robot;
    private JPanel panel; // Promoted to field to access dimensions

    /**
     * Constructor for the Window class, where we initialize the JFrame, set it to
     * fullscreen, and prepare the canvas for rendering.
     * 
     * @param title
     * @param width
     * @param height
     */
    public SwingWindow(String title, int width, int height) {
        this.originalWidth = width;
        this.originalHeight = height;
        frame = new JFrame(title);
        frame.setUndecorated(true); // Removes borders and title bar
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        canvasPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        this.panel = new JPanel() {
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
        panel.setFocusable(true); // Allows panel to receive KeyEvents
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
    
    public int getWidth() { return panel.getWidth(); }
    public int getHeight() { return panel.getHeight(); }

    public void releaseContext() {
        //do nothing
    }

    public void makeContextCurrent() {
        //do nothing
    }

    @Override
    public boolean isFocused() {
        // Retorna true se a janela (ou qualquer componente dela, como o painel) estiver ativa pelo usuário
        return frame.isActive();
    }

    @Override
    public Point getLocationOnScreen() {
        return frame.getLocationOnScreen();
    }

    @Override
    public void toggleFullscreen() {
        frame.dispose(); // Required to change decorated status
        
        boolean isCurrentlyUndecorated = frame.isUndecorated();
        frame.setUndecorated(!isCurrentlyUndecorated);

        if (!isCurrentlyUndecorated) { // Switch to Fullscreen (Undecorated)
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else { // Switch to Windowed (Decorated)
            frame.setExtendedState(JFrame.NORMAL);
            frame.setSize(originalWidth, originalHeight);
            frame.setLocationRelativeTo(null);
        }
        
        frame.setVisible(true);
        panel.requestFocus(); // Focus on the component that has the listeners
        
        // Use invokeLater to ensure window layout/insets are updated before centering
        SwingUtilities.invokeLater(this::centerMouse);
    }

    @Override
    public int getMouseDeltaX(int mouseX, int windowCenterX) {
        // Converte o centro relativo (ex: 400) para absoluto da tela (ex: 960)
        // para comparar corretamente com o mouseX (que é absoluto)
        if (panel != null && panel.isShowing()) {
            Point center = new Point(windowCenterX, 0);
            SwingUtilities.convertPointToScreen(center, panel);
            return mouseX - center.x;
        }
        return 0;
    }

    @Override
    public int getMouseDeltaY(int mouseY, int windowCenterY) {
        if (panel != null && panel.isShowing()) {
            Point center = new Point(0, windowCenterY);
            SwingUtilities.convertPointToScreen(center, panel);
            return mouseY - center.y;
        }
        return 0;
    }

    @Override
    public boolean isKeyDown(InputManager input, int keyCode) {
        return input.isKeyHeld(keyCode);
    }

    @Override
    public boolean isKeyPressedOnce(InputManager input, int keyCode) {
        return input.isKeyPressed(keyCode);
    }

    @Override
    public boolean isKeyDown(Toolkit defaultToolkit, int vkCapsLock) {
        try {
            return defaultToolkit.getLockingKeyState(vkCapsLock);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void centerMouse() {
        if (robot != null && isFocused()) {
            // Calculates the center relative to the Panel (Content Area)
            // This ensures alignment with MouseEvents which originate from the Panel source
            int centerX = panel.getWidth() / 2;
            int centerY = panel.getHeight() / 2;
            Point dest = new Point(centerX, centerY);
            
            // Converts local point (Panel) to absolute screen coordinates
            SwingUtilities.convertPointToScreen(dest, panel);
            
            robot.mouseMove(dest.x, dest.y);
        }
    }

    @Override
    public void addInputListener(InputManager input) {
        // Add listeners to PANEL (Content), not Frame.
        // This ensures mouse coordinates exclude window borders/title bar.
        panel.addKeyListener(input);
        panel.addMouseMotionListener(input);
        panel.addMouseWheelListener(input);
    }

    @Override
    public void requestFocus() {
        if (panel != null) panel.requestFocus();
    }
}