package client;

import common.GameInterface;
import common.PlayerCallback;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameClient implements PlayerCallback {
    private GameInterface game;
    private String playerSymbol;
    private final String playerName;
    private boolean myTurn = false;
    private boolean wantsToPlayAgain = false;
    private boolean gameInProgress = false;
    private String gameSessionId = null;
    private String opponentName = null;
    
    // Callbacks for the UI
    private Consumer<String[][]> boardUpdateCallback;
    private Consumer<String> statusCallback;
    private Consumer<String> gameOverCallback;
    private Runnable turnCallback;
    private Runnable newOpponentCallback;
    private Supplier<Boolean> restartGameCallback;

    public GameClient(String host, String playerName) throws RemoteException {
        this.playerName = playerName;
        this.game = connectToServer(host);
    }

    private GameInterface connectToServer(String host) throws RemoteException {
        int retries = 3;
        while (retries > 0) {
            try {
                Registry registry = LocateRegistry.getRegistry(host);
                GameInterface serverGame = (GameInterface) registry.lookup("GameService");
                PlayerCallback callbackStub = (PlayerCallback) UnicastRemoteObject.exportObject(this, 0);
                
                if (serverGame.joinGame(callbackStub, playerName)) {
                    return serverGame;
                }
                throw new RemoteException("Game is full");
            } catch (Exception e) {
                retries--;
                if (retries == 0) {
                    notifyStatus("Failed to connect after 3 attempts: " + e.getMessage());
                    throw new RemoteException("Connection failed", e);
                }
                try {
                    Thread.sleep(1000); // Wait before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RemoteException("Connection failed");
    }

    // RMI Callback methods
    @Override
    public void updateBoard(String[][] board) throws RemoteException {
        if (boardUpdateCallback != null) {
            boardUpdateCallback.accept(board);
        }
    }

    @Override
    public void notifyTurn(String playerSymbol) throws RemoteException {
        this.myTurn = playerSymbol.equals(this.playerSymbol);
        if (this.myTurn && gameInProgress) {
            notifyStatus("It's your turn! (" + this.playerSymbol + ")");
            if (turnCallback != null) {
                turnCallback.run();
            }
        } else if (gameInProgress) {
            notifyStatus("Waiting for opponent's move...");
        }
    }

    @Override
    public void gameOver(String winner) throws RemoteException {
        this.myTurn = false;
        this.gameInProgress = false;
        String message;
        
        if (winner == null) {
            message = "It's a draw!";
        } else if (winner.equals(playerSymbol)) {
            message = "You won!";
        } else {
            message = "You lost!";
        }
        
        if (gameOverCallback != null) {
            gameOverCallback.accept(message);
        }
    }

    @Override
    public void showMessage(String message) throws RemoteException {
        notifyStatus(message);
    }

    @Override
    public boolean ping() throws RemoteException {
        return true; // Simply return true to indicate the client is alive
    }

    @Override
    public void assignSymbol(String symbol) throws RemoteException {
        this.playerSymbol = symbol;
    }

    @Override
    public String getPlayerSymbol() throws RemoteException {
        return playerSymbol;
    }

    @Override
    public boolean wantsToPlayAgain() throws RemoteException {
        if (restartGameCallback != null) {
            wantsToPlayAgain = restartGameCallback.get();
        }
        return wantsToPlayAgain;
    }

    @Override
    public void promptForRestart() throws RemoteException {
        if (statusCallback != null) {
            statusCallback.accept("Game over. Do you want to play again?");
        }
    }

    @Override
    public void promptForNewOpponent() throws RemoteException {
        this.gameInProgress = false;
        this.myTurn = false;
        
        if (statusCallback != null) {
            statusCallback.accept("Your opponent disconnected. Waiting for a new opponent...");
        }
        
        
        if (newOpponentCallback != null) {
            newOpponentCallback.run();
        }
    }

    @Override
    public void setGameSession(String sessionId) throws RemoteException {
        this.gameSessionId = sessionId;
        this.gameInProgress = true;
        notifyStatus("Joined game session: " + sessionId);
    }

    @Override
    public void setOpponentInfo(String opponentName) throws RemoteException {
        this.opponentName = opponentName;
        notifyStatus("Playing against: " + opponentName);
    }

    @Override
    public String getGameSessionId() throws RemoteException {
        return gameSessionId;
    }

    // Server interaction methods
    public boolean makeMove(int x, int y) throws RemoteException {
        try {
            if (!gameInProgress) {
                notifyStatus("No active game in progress!");
                return false;
            }
            
            if (!myTurn) {
                notifyStatus("It's not your turn!");
                return false;
            }
            
            if (x < 0 || x > 2 || y < 0 || y > 2) {
                notifyStatus("Invalid position!");
                return false;
            }
            
            // Add visual feedback that move is being processed
            notifyStatus("Processing your move...");
            
            boolean validMove = game.makeMove(x, y, playerSymbol, gameSessionId);
            if (validMove) {
                myTurn = false;
                return true;
            } else {
                notifyStatus("Invalid move - try again");
                return false;
            }
        } catch (RemoteException e) {
            notifyStatus("Connection error: " + e.getMessage());
            // Attempt to reconnect
            try {
                this.game = connectToServer("localhost"); // or server IP
                notifyStatus("Reconnected to server");
            } catch (Exception ex) {
                notifyStatus("Failed to reconnect: " + ex.getMessage());
            }
            return false;
        }
    }

    public void disconnect() {
        try {
            if (game != null) {
                game.leaveGame(this);
                UnicastRemoteObject.unexportObject(this, true);
                this.gameInProgress = false;
            }
        } catch (Exception e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    public void requestNewGame() throws RemoteException {
        if (restartGameCallback != null) {
            wantsToPlayAgain = restartGameCallback.get();
        }
        if (gameSessionId != null) {
            game.restartGame(gameSessionId);
            notifyStatus("Game restart requested...");
        } else {
            notifyStatus("No active game session to restart");
        }
    }

    // UI callback registration methods
    public void setBoardUpdateCallback(Consumer<String[][]> callback) {
        this.boardUpdateCallback = callback;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setGameOverCallback(Consumer<String> callback) {
        this.gameOverCallback = callback;
    }

    public void setTurnCallback(Runnable callback) {
        this.turnCallback = callback;
    }

    public void setNewOpponentCallback(Runnable callback) {
        this.newOpponentCallback = callback;
    }
    
    public void setRestartGameCallback(Supplier<Boolean> callback) {
        this.restartGameCallback = callback;
    }

    // Getters
    public String getPlayerName() {
        return playerName;
    }

    public boolean isMyTurn() {
        return myTurn;
    }
    
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    // Utility method for notifications
    private void notifyStatus(String message) {
        if (statusCallback != null) {
            statusCallback.accept(message);
        }
        System.out.println("[Client] " + message);
    }
    
    // Method to leave the game cleanly
    public void leaveGame() throws RemoteException {
        if (game != null) {
            game.leaveGame(this);
            this.gameInProgress = false;
        }
    }
}