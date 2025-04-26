package client;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import javax.swing.*;

public class ClientUI extends JFrame {

    private final GameClient gameClient;
    private final JButton[][] buttons = new JButton[3][3];
    private final JLabel statusLabel = new JLabel("Waiting for connection...");
    private final JLabel playerInfoLabel = new JLabel();

    public ClientUI(GameClient client) {
        this.gameClient = client;
        initializeUI();
        setupCallbacks();
    }

    private void initializeUI() {
        setTitle("Tic-Tac-Toe RMI - Player: " + gameClient.getPlayerName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLayout(new BorderLayout());

        // Player info panel
        JPanel infoPanel = new JPanel();
        playerInfoLabel.setText("Player: " + gameClient.getPlayerName());

        infoPanel.add(playerInfoLabel);

        // Game board panel
        JPanel gamePanel = new JPanel(new GridLayout(3, 3));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                final int x = i, y = j;
                buttons[i][j].addActionListener(e -> makeMove(x, y));
                gamePanel.add(buttons[i][j]);
            }
        }

        // Control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        JButton restartButton = new JButton("New Game");
        restartButton.addActionListener(e -> restartGame());

        JPanel statusPanel = new JPanel();
        statusPanel.add(statusLabel);

        controlPanel.add(statusPanel, BorderLayout.CENTER);
        controlPanel.add(restartButton, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    gameClient.leaveGame();
                } catch (RemoteException ex) {
                    System.err.println("Error leaving game: " + ex.getMessage());
                }
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void setupCallbacks() {
        gameClient.setBoardUpdateCallback(this::updateBoard);
        gameClient.setStatusCallback(this::setStatus);
        gameClient.setGameOverCallback(this::gameOver);
        gameClient.setTurnCallback(() -> {
            try {
                playerInfoLabel.setText("Player: " + gameClient.getPlayerName() + " (" + gameClient.getPlayerSymbol() + ")");
                setStatus("It's your turn!");
                setBoardEnabled(true);
            } catch (RemoteException e) {
                setStatus("Connection error - unable to determine turn");
                showError("Connection error: " + e.getMessage());
            }
        });

        gameClient.setNewOpponentCallback(() -> {
            // Handle opponent disconnection
            SwingUtilities.invokeLater(() -> {
                resetBoard();
                setBoardEnabled(false);
                try {
                    // Update player info with current symbol (may change when new match occurs)
                    playerInfoLabel.setText("Player: " + gameClient.getPlayerName()
                            + " (" + gameClient.getPlayerSymbol() + ")");
                } catch (RemoteException e) {
                    playerInfoLabel.setText("Player: " + gameClient.getPlayerName());
                }
                setStatus("Your opponent disconnected. Waiting for a new opponent...");

                // Optionally show a dialog to inform the player
                JOptionPane.showMessageDialog(this,
                        "Your opponent has disconnected.\nWaiting for a new opponent...",
                        "Opponent Left",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        });

        gameClient.setRestartGameCallback(this::askToPlayAgain);
    }

    public void setBoardEnabled(boolean enabled) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setEnabled(enabled && buttons[i][j].getText().isEmpty());
            }
        }
    }

    private void makeMove(int x, int y) {
        try {
            if (gameClient.isMyTurn() && gameClient.makeMove(x, y)) {
                buttons[x][y].setText(gameClient.getPlayerSymbol());
                buttons[x][y].setEnabled(false);
            }
        } catch (RemoteException e) {
            showError("Connection error: " + e.getMessage());
        }
    }

    private void restartGame() {
        try {
            gameClient.requestNewGame();
            resetBoard();
            setStatus("New game requested...");
        } catch (RemoteException e) {
            showError("Error: " + e.getMessage());
        }
    }

    public void updateBoard(String[][] board) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    buttons[i][j].setText(board[i][j] == null || board[i][j].isEmpty() ? "" : board[i][j]);
                    buttons[i][j].setEnabled(gameClient.isMyTurn() && (board[i][j] == null || board[i][j].isEmpty()));
                }
            }
        });
    }

    public void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public void gameOver(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message);
            setStatus(message);
            setBoardEnabled(false);
        });
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
            }
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean askToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(this,
                "Do you want to play again?", "Game Over",
                JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }
}
