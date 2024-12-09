package com.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static PrintWriter logWriter;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000);
             PrintWriter logWriterTemp = new PrintWriter(new BufferedWriter(new FileWriter("chat_log.txt", true)), true)) {

            logWriter = logWriterTemp;
            System.out.println("Server is running");

            while (true) {
                acceptClientConnection(serverSocket);
            }

        } catch (IOException e) {
            System.err.println("Error starting server or initializing log writer: " + e.getMessage());
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
        // Write the message to the log file
        logWriter.println(message);
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}

