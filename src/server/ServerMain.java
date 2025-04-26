package server;

import common.GameInterface;
import factory.GameFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {

    private static final int RMI_PORT = 1099;
    private static final String SERVICE_NAME = "GameService";
    
    private static final int MAX_PLAYERS = 10;

    public static void main(String[] args) {
        try {
            // Configuration de la sécurité    

            System.setProperty("java.security.policy", "security.policy");
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            String serverIP = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Serveur prêt sur : rmi://" + serverIP + ":" + RMI_PORT + "/" + SERVICE_NAME);

            System.setProperty("java.rmi.server.codebase", "http://"+ serverIP + "/classes/");
            
            GameInterface game = GameFactory.createGame(MAX_PLAYERS);
            
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            registry.rebind(SERVICE_NAME, game);
            System.out.println("Serveur prêt sur : rmi://" + serverIP + ":" + RMI_PORT + "/" + SERVICE_NAME);
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Erreur de démarrage du serveur :");
            e.printStackTrace();
            System.exit(1);
        }
    }
}