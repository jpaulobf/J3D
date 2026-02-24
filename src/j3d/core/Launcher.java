package j3d.core;

public class Launcher {
    public static void main(String[] args) {
        Game game = new Game();
        new Thread(game).start();
    }
}