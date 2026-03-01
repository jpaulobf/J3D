package j3d.core;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Window class responsible for creating the game window, managing the canvas
 * for rendering, and displaying the FPS counter.
 */
public class Window {
    private JFrame frame;
    private BufferedImage canvas;
    private int[] canvasPixels;

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
        frame.setUndecorated(true); // Remove bordas e barra de título
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        canvasPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Não chamamos super.paintComponent(g) para evitar limpar a tela
                // desnecessariamente
                Graphics2D g2d = (Graphics2D) g;

                // Hints para priorizar velocidade na transferência da imagem para a tela
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                System.setProperty("sun.java2d.d3d", "true");

                // Escala a imagem do buffer para o tamanho total da janela (Fullscreen)
                g2d.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);

                // Sincroniza com o display hardware para evitar tearing e lag visual
                Toolkit.getDefaultToolkit().sync();
            }
        };
        panel.setPreferredSize(new Dimension(width, height));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximiza a janela
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Atualiza os pixels do canvas com os dados do renderer e o FPS atual, e solicita a repintura da tela.
     * @param rendererPixels
     */
    public void update(int[] rendererPixels) {
        System.arraycopy(rendererPixels, 0, canvasPixels, 0, rendererPixels.length);
        frame.repaint();
    }

    /**
     * Getter para o JFrame, necessário para capturar eventos de input e obter a posição do mouse.
     * @return
     */
    public JFrame getFrame() {
        return frame;
    }
}