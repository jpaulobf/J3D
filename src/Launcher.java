import javax.swing.JFrame;

import renderer.SoftwareRenderer;

/**
 * Launcher class to start the software renderer.
 */
public class Launcher {

    /**
     * Main method to start the software renderer.
     * 
     * @param args
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Engine 3D");
        SoftwareRenderer r = new SoftwareRenderer();
        f.add(r);
        f.pack();
        f.setDefaultCloseOperation(3);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        new Thread(r).start();
    }

}
