package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PlayerCallback extends Remote {
    void showMessage(String message) throws RemoteException;
    void assignSymbol(String symbol) throws RemoteException;
    void notifyTurn(String symbol) throws RemoteException;
    void updateBoard(String[][] board) throws RemoteException;
    void gameOver(String winner) throws RemoteException;
    void promptForRestart() throws RemoteException;
    boolean wantsToPlayAgain() throws RemoteException;
    String getPlayerSymbol() throws RemoteException;
    void promptForNewOpponent() throws RemoteException;
    boolean ping() throws RemoteException;
    void setGameSession(String sessionId) throws RemoteException; // New method
    void setOpponentInfo(String opponentName) throws RemoteException; // New method
    String getGameSessionId() throws RemoteException; // New method
}