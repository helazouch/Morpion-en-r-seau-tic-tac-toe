package server;

import common.GameState;
import common.PlayerCallback;
import java.rmi.RemoteException;
import java.security.SecureRandom;

public class GameSession {
    private final PlayerCallback player1;
    private final PlayerCallback player2;
    private final GameState gameState;
    private final String player1Symbol;
    private final String player2Symbol;
    private final SecureRandom random;
    private boolean player1WantsRestart;
    private boolean player2WantsRestart;

    public GameSession(PlayerCallback player1, PlayerCallback player2, 
                      String player1Symbol, String player2Symbol) throws RemoteException {
        this.player1 = player1;
        this.player2 = player2;
        this.player1Symbol = player1Symbol;
        this.player2Symbol = player2Symbol;
        this.gameState = new GameState();
        this.random = new SecureRandom();
        resetBoard();
    }

    public void resetBoard() {
        gameState.resetGame();
        // Determine randomly who starts
        gameState.setCurrentPlayer(random.nextBoolean() ? player1Symbol : player2Symbol);
        this.player1WantsRestart = false;
        this.player2WantsRestart = false;
    }

    public boolean containsPlayer(PlayerCallback player) throws RemoteException {
        return player.equals(player1) || player.equals(player2);
    }

    public PlayerCallback getOpponent(PlayerCallback player) throws RemoteException {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }
    
    public PlayerCallback getPlayer1() {
        return player1;
    }
    
    public PlayerCallback getPlayer2() {
        return player2;
    }

    public boolean isGameOver() {
        return gameState.isGameOver();
    }

    public synchronized boolean makeMove(int x, int y, String playerSymbol) throws RemoteException {
        if (gameState.isGameOver()) {
            notifyPlayer(playerSymbol, "Game is already over");
            return false;
        }
        
        if (!playerSymbol.equals(gameState.getCurrentPlayer())) {
            notifyPlayer(playerSymbol, "It's not your turn!");
            return false;
        }
        
        // Validate move coordinates
        if (x < 0 || x >= 3 || y < 0 || y >= 3) {
            notifyPlayer(playerSymbol, "Invalid coordinates!");
            return false;
        }
        
        if (!gameState.isCellEmpty(x, y)) {
            notifyPlayer(playerSymbol, "That position is already taken!");
            return false;
        }
        
        // Process move
        gameState.makeMove(x, y, playerSymbol);
        String playerName = getPlayerName(playerSymbol);
        notifyAllPlayers(String.format("%s played at position (%d,%d)", playerName, x+1, y+1));
        
        // Check game state
        if (gameState.checkWin()) {
            handleWin(playerSymbol);
            return true;
        }
        
        if (gameState.isBoardFull()) {
            handleDraw();
            return true;
        }
        
        switchPlayer();
        return true;
    }

    private String getPlayerName(String symbol) {
        if (symbol.equals(player1Symbol)) {
            try {
                return "Player 1";
            } catch (Exception e) {
                return "Player 1";
            }
        } else {
            try {
                return "Player 2";
            } catch (Exception e) {
                return "Player 2";
            }
        }
    }

    private void handleWin(String winnerSymbol) throws RemoteException {
        gameState.setGameOver(true);
        gameState.setWinner(winnerSymbol);
        String playerName = getPlayerName(winnerSymbol);
        notifyAllPlayers(playerName + " (" + winnerSymbol + ") has won!");
        notifyGameOver();
    }

    private void handleDraw() throws RemoteException {
        gameState.setGameOver(true);
        notifyAllPlayers("It's a draw! The board is full.");
        notifyGameOver();
    }

    private void switchPlayer() throws RemoteException {
        gameState.switchPlayer();
        notifyCurrentPlayer();
    }

    public void notifyCurrentPlayer() throws RemoteException {
        try {
            player1.notifyTurn(gameState.getCurrentPlayer().equals(player1Symbol) ? player1Symbol : "");
            player1.updateBoard(getBoardState());
        } catch (RemoteException e) {
            System.err.println("Error notifying player 1: " + e.getMessage());
        }
        
        try {
            player2.notifyTurn(gameState.getCurrentPlayer().equals(player2Symbol) ? player2Symbol : "");
            player2.updateBoard(getBoardState());
        } catch (RemoteException e) {
            System.err.println("Error notifying player 2: " + e.getMessage());
        }
    }

    private void notifyAllPlayers(String message) throws RemoteException {
        System.out.println("Game message: " + message);

        try {
            player1.showMessage(message);
            player1.updateBoard(getBoardState());
        } catch (RemoteException e) {
            System.err.println("Error notifying player 1: " + e.getMessage());
        }
        
        try {
            player2.showMessage(message);
            player2.updateBoard(getBoardState());
        } catch (RemoteException e) {
            System.err.println("Error notifying player 2: " + e.getMessage());
        }
    }

    private void notifyPlayer(String playerSymbol, String message) throws RemoteException {
        PlayerCallback player = null;
        
        if (playerSymbol.equals(player1Symbol)) {
            player = player1;
        } else if (playerSymbol.equals(player2Symbol)) {
            player = player2;
        }
        
        if (player != null) {
            try {
                player.showMessage(message);
            } catch (RemoteException e) {
                System.err.println("Error notifying player: " + e.getMessage());
            }
        }
    }

    private void notifyGameOver() throws RemoteException {
        try {
            player1.gameOver(gameState.getWinner());
            player1.promptForRestart();
        } catch (RemoteException e) {
            System.err.println("Error notifying player 1 of game over: " + e.getMessage());
        }
        
        try {
            player2.gameOver(gameState.getWinner());
            player2.promptForRestart();
        } catch (RemoteException e) {
            System.err.println("Error notifying player 2 of game over: " + e.getMessage());
        }
    }

    public String[][] getBoardState() {
        return gameState.getBoard();
    }

    public void restartGame() throws RemoteException {
        resetBoard();
        
        // Clear game over state on clients and ensure game is active
        try {
            player1.setGameSession(player1.getGameSessionId());
            player2.setGameSession(player2.getGameSessionId());
        } catch (RemoteException e) {
            System.err.println("Error resetting game session state: " + e.getMessage());
        }
        
        notifyAllPlayers("Game restarted!");
        notifyCurrentPlayer();
    }

}