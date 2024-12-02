package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private static final Set<Socket> clients = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server is running");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    clients.add(socket);
                    System.out.println(socket.getInetAddress() + " is connected");

                    new Thread(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                            String message;
                            while ((message = in.readLine()) != null && !"!exit".equals(message)) {
                                synchronized (clients) {
                                    for (Socket client : clients) {
                                        if (!client.isClosed()) {
                                            try {
                                                PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                                                clientOut.println(message);
                                            } catch (IOException e) {
                                                System.err.println("Error sending message to client: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading from client: " + e.getMessage());
                        } finally {
                            synchronized (clients) {
                                clients.remove(socket);
                            }
                            try {
                                socket.close();
                            } catch (IOException e) {
                                System.err.println("Error closing client socket: " + e.getMessage());
                            }
                        }
                    }).start();

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error starting server on port 5000: " + e.getMessage());
        }
    }
}