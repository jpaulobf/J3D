package j3d.core;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Window {
    private JFrame frame;
    private BufferedImage canvas;
    private int[] canvasPixels;

    public Window(String title, int width, int height) {
        frame = new JFrame(title);
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        canvasPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Não chamamos super.paintComponent(g) para evitar limpar a tela desnecessariamente
                Graphics2D g2d = (Graphics2D) g;
                
                // Hints para priorizar velocidade na transferência da imagem para a tela
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                System.setProperty("sun.java2d.d3d", "true");
                
                g2d.drawImage(canvas, 0, 0, null);
                
                // Sincroniza com o display hardware para evitar tearing e lag visual
                Toolkit.getDefaultToolkit().sync();
            }
        };
        panel.setPreferredSize(new Dimension(width, height));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void update(int[] rendererPixels) {
        System.arraycopy(rendererPixels, 0, canvasPixels, 0, rendererPixels.length);
        frame.repaint();
    }

    public JFrame getFrame() { return frame; }
}