package factory;

import common.GameInterface;
import server.GameImpl;

public class GameFactory {
    public static GameInterface createGame(int maxPlayers) {
        try {
            return new GameImpl(maxPlayers);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du jeu", e);
        }
    }
}
