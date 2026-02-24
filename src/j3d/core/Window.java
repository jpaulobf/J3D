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
                super.paintComponent(g);
                g.drawImage(canvas, 0, 0, null);
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