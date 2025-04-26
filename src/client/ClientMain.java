package client;

import java.rmi.RemoteException;
import javax.swing.JOptionPane;

public class ClientMain {

    private static final String DEFAULT_HOST = "localhost";

    public static void main(String[] args) {
        try {
            System.setProperty("java.security.policy", "security.policy");
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            } 

            String host = DEFAULT_HOST;
            String playerName = getPlayerName(args);
            if (playerName == null || playerName.trim().isEmpty()) {
                System.out.println("Le nom du joueur est requis");
                return;
            }
            if (args.length >= 2) {
                host = args[0];
            } else {
                String hostInput = JOptionPane.showInputDialog(null, "Entrez l'adresse du serveur:", "localhost");
                if (hostInput != null && !hostInput.trim().isEmpty()) {
                    host = hostInput;
                }
            }
            // Création du client et de l'interface
            GameClient gameClient = new GameClient(host, playerName);
            ClientUI clientUI = new ClientUI(gameClient);
            // Configuration des callbacks  
            configureCallbacks(gameClient, clientUI);
        } catch (RemoteException e) {
            showErrorDialog("Erreur de connexion au serveur:\n" + e.getMessage());
        } catch (Exception e) {
            showErrorDialog("Erreur inattendue:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getPlayerName(String[] args) {
        if (args.length >= 1) {
            return args[args.length - 1];
            // Le dernier argument est toujours le nom   
        }
        return JOptionPane.showInputDialog(null, "Entrez votre nom:", "Connexion au jeu de Morpion", JOptionPane.PLAIN_MESSAGE);
    }

    private static void configureCallbacks(GameClient gameClient, ClientUI clientUI) {
        gameClient.setBoardUpdateCallback(clientUI::updateBoard);
        gameClient.setStatusCallback(clientUI::setStatus);
        gameClient.setGameOverCallback(clientUI::gameOver);
        gameClient.setTurnCallback(() -> {
            try {
                clientUI.setStatus("C'est votre tour! (" + gameClient.getPlayerSymbol() + ")");
                clientUI.setBoardEnabled(true);
            } catch (RemoteException e) {
                clientUI.setStatus("Erreur de connexion - impossible de déterminer le tour");
                clientUI.showError("Erreur de connexion: " + e.getMessage());
            }
        });
    }

    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
