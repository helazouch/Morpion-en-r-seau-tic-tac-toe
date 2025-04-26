package common;

import java.io.Serializable;

public class GameState implements Serializable {
    private String[][] board;
    private String currentPlayer;
    private String winner;
    private boolean gameOver;

    // Constructeur
    public GameState() {
        this.board = new String[3][3];
        this.currentPlayer = "X"; // X commence toujours
        this.winner = null;
        this.gameOver = false;
        initializeBoard();
    }

    // Initialise la grille avec des cases vides
    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
            }
        }
    }

    // Getters et Setters
    public String[][] getBoard() {
        return board;
    }

    public void setBoard(String[][] board) {
        this.board = board;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    // Méthode pour changer de joueur
    public void switchPlayer() {
        this.currentPlayer = (this.currentPlayer.equals("X")) ? "O" : "X";
    }

    // Méthode pour vérifier si une case est vide
    public boolean isCellEmpty(int row, int col) {
        return board[row][col].isEmpty();
    }

    // Méthode pour placer un symbole sur la grille
    public void makeMove(int row, int col, String symbol) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            board[row][col] = symbol;
        }
    }

    // Méthode pour vérifier s'il y a un gagnant
    public boolean checkWin() {
        // Vérification des lignes et colonnes
        for (int i = 0; i < 3; i++) {
            // Lignes
            if (!board[i][0].isEmpty() && board[i][0].equals(board[i][1]) && board[i][0].equals(board[i][2])) {
                this.winner = board[i][0];
                return true;
            }
            // Colonnes
            if (!board[0][i].isEmpty() && board[0][i].equals(board[1][i]) && board[0][i].equals(board[2][i])) {
                this.winner = board[0][i];
                return true;
            }
        }
        
        // Diagonales
        if (!board[0][0].isEmpty() && board[0][0].equals(board[1][1]) && board[0][0].equals(board[2][2])) {
            this.winner = board[0][0];
            return true;
        }
        if (!board[0][2].isEmpty() && board[0][2].equals(board[1][1]) && board[0][2].equals(board[2][0])) {
            this.winner = board[0][2];
            return true;
        }
        
        return false;
    }

    // Méthode pour vérifier si la grille est pleine (match nul)
    public boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Méthode pour réinitialiser le jeu
    public void resetGame() {
        initializeBoard();
        this.currentPlayer = "X";
        this.winner = null;
        this.gameOver = false;
    }

    // Représentation textuelle de l'état du jeu (pour le débogage)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Player: ").append(currentPlayer).append("\n");
        sb.append("Winner: ").append(winner != null ? winner : "None").append("\n");
        sb.append("Board:\n");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(board[i][j].isEmpty() ? "-" : board[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}