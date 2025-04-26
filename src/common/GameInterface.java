package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameInterface extends Remote {
    boolean joinGame(PlayerCallback callback, String playerName) throws RemoteException;
    boolean makeMove(int x, int y, String playerSymbol, String sessionId) throws RemoteException;
    String[][] getBoard() throws RemoteException; // Kept for backward compatibility
    String getCurrentPlayer() throws RemoteException; // Kept for backward compatibility
    void restartGame(String sessionId) throws RemoteException;
    void leaveGame(PlayerCallback player) throws RemoteException;
}