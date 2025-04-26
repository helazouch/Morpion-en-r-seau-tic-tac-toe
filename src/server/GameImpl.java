package server;

import common.GameInterface;
import common.PlayerCallback;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameImpl extends UnicastRemoteObject implements GameInterface {
    private final Map<String, GameSession> gameSessions;
    private final Map<PlayerCallback, Long> lastActivity;
    private final List<PlayerCallback> waitingPlayers;
    private final Map<PlayerCallback, String> playerNames;
    private final Map<PlayerCallback, String> playerSymbols;
    private final Map<PlayerCallback, String> playerSessions;
    private final int maxPlayers;
    private final SecureRandom random;
    private final ScheduledExecutorService scheduler;
    
    public GameImpl(int maxPlayers) throws RemoteException {
        super();
        this.gameSessions = new ConcurrentHashMap<>();
        this.lastActivity = new ConcurrentHashMap<>();
        this.waitingPlayers = new ArrayList<>();
        this.playerNames = new HashMap<>();
        this.playerSymbols = new HashMap<>();
        this.playerSessions = new HashMap<>();
        this.maxPlayers = maxPlayers;
        this.random = new SecureRandom();
        
        // Start periodic cleanup task
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(this::cleanupInactivePlayers, 30, 30, TimeUnit.SECONDS);
    }


    
    @Override
    public synchronized boolean joinGame(PlayerCallback callback, String playerName) throws RemoteException {
        System.out.println("Player joined: " + playerName);
        
        if (getPlayerCount() >= maxPlayers) {
            callback.showMessage("Game is full (maximum " + maxPlayers + " players)");
            return false;
        }
        
        // Store player name and update last activity
        playerNames.put(callback, playerName);
        lastActivity.put(callback, System.currentTimeMillis());
        
        // Add player to waiting list first, don't assign symbol yet
        waitingPlayers.add(callback);
        
        // Check if we can start a game
        if (waitingPlayers.size() >= 2) {
            startNewGame();
        } else {
            // Only when waiting, temporarily assign a symbol
            // Note: This symbol may change when matched with another player
            String tempSymbol = random.nextBoolean() ? "X" : "O";
            playerSymbols.put(callback, tempSymbol);
            callback.assignSymbol(tempSymbol);
            callback.showMessage("Welcome " + playerName + "! You're playing as " + tempSymbol + " (may change when matched)");
            callback.showMessage("Waiting for another player to join...");
        }
        
        return true;
    }
    
    private synchronized void startNewGame() throws RemoteException {
        // Remove any disconnected players first
        waitingPlayers.removeIf(player -> {
            try {
                player.ping();
                return false;
            } catch (RemoteException e) {
                cleanupPlayer(player);
                return true;
            }
        });
    
        if (waitingPlayers.size() < 2) return;
    
        PlayerCallback player1 = waitingPlayers.remove(0);
        PlayerCallback player2 = waitingPlayers.remove(0);
    
        // Verify both players are still connected
        try {
            player1.ping();
            player2.ping();
        } catch (RemoteException e) {
            // If either player disconnected, put the remaining one back in queue
            if (player1 != null) {
                try { player1.ping(); waitingPlayers.add(0, player1); } catch (RemoteException ex) { cleanupPlayer(player1); }
            }
            if (player2 != null) {
                try { player2.ping(); waitingPlayers.add(0, player2); } catch (RemoteException ex) { cleanupPlayer(player2); }
            }
            return;
        }
    
        String player1Name = playerNames.get(player1);
        String player2Name = playerNames.get(player2);
        
        // Ensure players have complementary symbols
        String player1Symbol = random.nextBoolean() ? "X" : "O";
        String player2Symbol = player1Symbol.equals("X") ? "O" : "X";
        
        // Update the symbols in our map
        playerSymbols.put(player1, player1Symbol);
        playerSymbols.put(player2, player2Symbol);
        
        // Notify players of their final symbol
        player1.assignSymbol(player1Symbol);
        player2.assignSymbol(player2Symbol);
        
        // Create a new game session with a unique ID
        String sessionId = "game-" + UUID.randomUUID().toString();
        GameSession session = new GameSession(player1, player2, player1Symbol, player2Symbol);
        gameSessions.put(sessionId, session);
        
        // Associate players with their session
        playerSessions.put(player1, sessionId);
        playerSessions.put(player2, sessionId);
        
        // Set session info for both players
        player1.setGameSession(sessionId);
        player2.setGameSession(sessionId);
        
        // Set opponent info for both players
        player1.setOpponentInfo(player2Name);
        player2.setOpponentInfo(player1Name);
        
        player1.showMessage("Welcome " + player1Name + "! You're playing as " + player1Symbol);
        player1.showMessage("Game started against " + player2Name);
        
        player2.showMessage("Welcome " + player2Name + "! You're playing as " + player2Symbol);
        player2.showMessage("Game started against " + player1Name);
        
        // Update activity timestamps
        lastActivity.put(player1, System.currentTimeMillis());
        lastActivity.put(player2, System.currentTimeMillis());
        
        // Start the game
        session.notifyCurrentPlayer();
    }

    @Override
    public synchronized boolean makeMove(int x, int y, String playerSymbol, String sessionId) throws RemoteException {
        // Validate session ID
        if (sessionId == null || !gameSessions.containsKey(sessionId)) {
            System.err.println("Invalid session ID: " + sessionId);
            return false;
        }
        
        PlayerCallback player = findPlayerBySymbol(playerSymbol);
        if (player == null) {
            System.err.println("Player not found for symbol: " + playerSymbol);
            return false;
        }
        
        // Update last activity timestamp
        lastActivity.put(player, System.currentTimeMillis());
        
        // Get the correct session
        GameSession session = gameSessions.get(sessionId);
        if (session == null) {
            System.err.println("Session not found: " + sessionId);
            return false;
        }
        
        return session.makeMove(x, y, playerSymbol);
    }
    
    @Override
    public synchronized String[][] getBoard() throws RemoteException {
        // This method is now obsolete as each session has its own board
        // Return empty board for backward compatibility
        String[][] emptyBoard = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                emptyBoard[i][j] = "";
            }
        }
        return emptyBoard;
    }
    
    @Override
    public synchronized String getCurrentPlayer() throws RemoteException {
        // This method is now obsolete as each session has its current player
        // Return empty string for backward compatibility
        return "";
    }
    
    @Override
    public synchronized void restartGame(String sessionId) throws RemoteException {
        GameSession session = gameSessions.get(sessionId);
        if (session != null) {
            session.restartGame();
        } else {
            System.err.println("Cannot restart - session not found: " + sessionId);
        }
    }
    @Override
    public synchronized void leaveGame(PlayerCallback player) throws RemoteException {
        System.out.println("Player leaving: " + playerNames.getOrDefault(player, "Unknown"));
        String sessionId = playerSessions.get(player);
        
        if (sessionId != null) {
            GameSession session = gameSessions.get(sessionId);
            if (session != null) {
                try {
                    PlayerCallback opponent = session.getOpponent(player);
                    if (opponent != null) {
                        // Notify opponent about player leaving
                        String playerName = playerNames.getOrDefault(player, "Unknown");
                        opponent.showMessage("Player " + playerName + " has left the game.");
                        
                        // Set the opponent back to waiting state
                        opponent.promptForNewOpponent();
                        
                        // Add opponent back to waiting list
                        if (!waitingPlayers.contains(opponent)) {
                            waitingPlayers.add(opponent);
                            
                            // Update last activity timestamp
                            lastActivity.put(opponent, System.currentTimeMillis());
                            
                            // Keep the existing symbol for now, will be reassigned when matched
                            opponent.showMessage("Waiting for a new opponent...");
                        }
                    }
                } catch (RemoteException e) {
                    System.err.println("Error notifying opponent about player leaving: " + e.getMessage());
                    // The opponent is probably disconnected too, clean them up
                    PlayerCallback opponent = null;
                    try {
                        opponent = session.getOpponent(player);
                        if (opponent != null) {
                            cleanupPlayer(opponent);
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to get opponent: " + ex.getMessage());
                    }
                }
                
                // Remove the game session
                gameSessions.remove(sessionId);
            }
        }
        
        // Clean up player records
        cleanupPlayer(player);
        
        // Try to match remaining players
        if (waitingPlayers.size() >= 2) {
            try {
                startNewGame();
            } catch (Exception e) {
                System.err.println("Error starting new game after player left: " + e.getMessage());
            }
        }
    }
    
    private void cleanupPlayer(PlayerCallback player) {
        waitingPlayers.remove(player);
        playerNames.remove(player);
        playerSymbols.remove(player);
        playerSessions.remove(player);
        lastActivity.remove(player);
    }
    
    private PlayerCallback findPlayerBySymbol(String symbol) {
        for (Map.Entry<PlayerCallback, String> entry : playerSymbols.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(symbol)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private int getPlayerCount() {
        return waitingPlayers.size() + getActivePlayers();
    }
    
    private int getActivePlayers() {
        int count = 0;
        for (GameSession session : gameSessions.values()) {
            // Each session has 2 players
            count += 2;
        }
        return count;
    }

    private void cleanupInactivePlayers() {
        System.out.println("Running inactive player cleanup...");
        long now = System.currentTimeMillis();
        
        // Check waiting players first
        waitingPlayers.removeIf(player -> {
            Long lastActive = lastActivity.get(player);
            if (lastActive == null || now - lastActive > 60000) { // 60 second timeout
                try {
                    player.ping(); // Try to ping before removing
                    lastActivity.put(player, System.currentTimeMillis());
                    return false;
                } catch (Exception e) {
                    System.out.println("Removing inactive waiting player");
                    cleanupPlayer(player);
                    return true;
                }
            }
            return false;
        });
        
        // Check active game sessions for inactive players
        List<String> sessionsToRemove = new ArrayList<>();
        
        for (Map.Entry<String, GameSession> entry : gameSessions.entrySet()) {
            String sessionId = entry.getKey();
            GameSession session = entry.getValue();
            
            try {
                PlayerCallback player1 = session.getPlayer1();
                PlayerCallback player2 = session.getPlayer2();
                
                boolean player1Active = isPlayerActive(player1, now);
                boolean player2Active = isPlayerActive(player2, now);
                
                if (!player1Active || !player2Active) {
                    // Handle inactive players in this session
                    handleInactiveGameSession(sessionId, session, player1, player2, player1Active, player2Active);
                    sessionsToRemove.add(sessionId);
                }
            } catch (Exception e) {
                System.err.println("Error checking session " + sessionId + ": " + e.getMessage());
                sessionsToRemove.add(sessionId);
            }
        }
        
        // Remove marked sessions
        for (String sessionId : sessionsToRemove) {
            gameSessions.remove(sessionId);
        }
    }
    
    private boolean isPlayerActive(PlayerCallback player, long now) {
        Long lastActive = lastActivity.get(player);
        if (lastActive == null || now - lastActive > 60000) { // 60 second timeout
            try {
                player.ping();
                lastActivity.put(player, now);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
    
    private void handleInactiveGameSession(String sessionId, GameSession session, 
                                         PlayerCallback player1, PlayerCallback player2,
                                         boolean player1Active, boolean player2Active) {
        try {
            if (player1Active && !player2Active) {
                player1.promptForNewOpponent();
                waitingPlayers.add(player1);
                cleanupPlayer(player2);
            } else if (!player1Active && player2Active) {
                player2.promptForNewOpponent();
                waitingPlayers.add(player2);
                cleanupPlayer(player1);
            } else {
                // Both inactive, clean up both
                cleanupPlayer(player1);
                cleanupPlayer(player2);
            }
        } catch (Exception e) {
            System.err.println("Error handling inactive session: " + e.getMessage());
        }
    }
}