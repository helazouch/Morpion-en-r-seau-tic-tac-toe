package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import common.GameInterface;
import factory.GameFactory;
import server.GameImpl;

public class GameServer {
    private static final int RMI_PORT = 1099;
    private static final String SERVICE_NAME = "GameService";
    private static final int MAX_PLAYERS = 10;

    public static void main(String[] args) {
        try {
            // Configuration de la sécurité RMI
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
                System.out.println("Security manager installed");
            }

            // Création du registry RMI
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            System.out.println("RMI Registry created on port " + RMI_PORT);

            // Création de l'instance de jeu
            GameInterface gameServer = GameFactory.createGame(MAX_PLAYERS);
            System.out.println("Game implementation initialized");

            // Exportation de l'objet distant
            GameInterface stub = gameServer;
            System.out.println("Remote object exported");

            // Enregistrement dans le registry
            registry.rebind(SERVICE_NAME, stub);
            System.out.println("Service '" + SERVICE_NAME + "' registered");

            System.out.println("Tic-Tac-Toe RMI Game Server ready!");
            System.out.println("Waiting for client connections...");

            // Garder le serveur actif
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}