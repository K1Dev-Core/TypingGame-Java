package server;

import java.io.IOException;

public class ServerLauncher {
    public static void main(String[] args) {
        System.out.println("=== Typing Game Server ===");
        System.out.println("Starting server on port 8888...");
        
        TypingGameServer server = new TypingGameServer();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\\nShutting down server...");
            try {
                server.stop();
            } catch (IOException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
            System.out.println("Server stopped.");
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}