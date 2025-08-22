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
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    NetworkMessage message = (NetworkMessage) input.readObject();
                    if (message != null && listener != null) {
                        listener.onMessageReceived(message);
                    }
                } catch (ClassCastException | ClassNotFoundException e) {
                    System.err.println("Message serialization error: " + e.getMessage());
                    System.err.println("Error details: " + e.getClass().getSimpleName());
                    if (e.getCause() != null) {
                        System.err.println("Caused by: " + e.getCause().getMessage());
                    }
                    System.err.println("Skipping corrupted message and continuing...");

                    continue;
                } catch (StreamCorruptedException e) {
                    System.err.println("Stream corruption detected: " + e.getMessage());
                    System.err.println("This usually indicates a synchronization issue on the server.");
                    System.err.println("Attempting to recover...");

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                } catch (EOFException e) {
                    System.err.println("Server closed connection unexpectedly: " + e.getMessage());
                    break;
                } catch (SocketException e) {
                    if (isConnected) {
                        System.err.println("Socket error: " + e.getMessage());
                    }
                    break;
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
            if (socket != null) {
                socket.close();
            }
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
