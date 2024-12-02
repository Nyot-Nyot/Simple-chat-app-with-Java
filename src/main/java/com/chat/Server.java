package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server is running");

            while (true) {
                acceptClientConnection(serverSocket);
            }

        } catch (IOException e) {
            System.err.println("Error starting server on port 5000: " + e.getMessage());
        }
    }

    private static void acceptClientConnection(ServerSocket serverSocket) {
        try {
            Socket clientSocket = serverSocket.accept();
            System.out.println(clientSocket.getInetAddress() + " is connected");

            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            new Thread(clientHandler).start();

        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
        }
    }

    private static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true); // Initialize the PrintWriter
            } catch (IOException e) {
                System.err.println("Error initializing output stream: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())); // Remove try-with-resources
                // Read the username from the client
                username = reader.readLine();
                if (username == null || username.trim().isEmpty()) {
                    System.err.println("Invalid username from client.");
                    socket.close();
                    return;
                }
                System.out.println(username + " has connected.");
                // Notify other clients
                broadcastMessage("Server: " + username + " has joined the chat.", this);

                String message;
                while ((message = reader.readLine()) != null && !"!exit".equals(message)) {
                    broadcastMessage(username + ": " + message, this);
                }
            } catch (IOException e) {
                System.err.println("Error reading from client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        public void sendMessage(String message) {
            out.println(message); // Use the existing PrintWriter
        }

        private void cleanup() {
            clients.remove(this);
            // Notify other clients
            broadcastMessage("Server: " + username + " has left the chat.", this);
            try {
                socket.close();
                System.out.println(socket.getInetAddress() + " is disconnected");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}