package j3d;

import j3d.core.Game;

/**
 * Launcher class responsible for starting the game by creating an instance of
 * the Game class and running it in a separate thread.
 */
public class Launcher {
    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }
}