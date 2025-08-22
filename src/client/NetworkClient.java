package client;

import java.io.*;
import java.net.*;
import shared.*;

public class NetworkClient {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean isConnected;
    private NetworkListener listener;
    private Thread readerThread;

    public interface NetworkListener {
        void onMessageReceived(NetworkMessage message);

        void onDisconnected();
    }

    public NetworkClient(NetworkListener listener) {
        this.listener = listener;
    }

    public boolean connect(String host, int port) {
        try {
            System.out.println("Attempting to connect to " + host + ":" + port);
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            readerThread = new Thread(this::readMessages);
            readerThread.start();

            System.out.println("Successfully connected to server");
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    private void readMessages() {
        try {
            while (isConnected) {
                try {
                    NetworkMessage message = (NetworkMessage) input.readObject();
                    if (listener != null) {
                        listener.onMessageReceived(message);
                    }
                } catch (ClassCastException | ClassNotFoundException | StreamCorruptedException e) {
                    System.err.println("Message serialization error: " + e.getMessage());
                    System.err.println("Attempting to continue...");
                    try {
                        input.reset();
                    } catch (IOException resetException) {
                        System.err.println("Failed to reset input stream: " + resetException.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        } finally {
            disconnect();
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (isConnected && output != null) {
            try {
                System.out.println("Sending message: " + message.type + ", data: " + message.data);
                output.writeObject(message);
                output.flush();
            } catch (IOException e) {
                System.err.println("Failed to send message: " + e.getMessage());
                disconnect();
            }
        } else {
            System.err.println("Cannot send message - not connected");
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (socket != null)
                socket.close();
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
