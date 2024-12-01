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

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server is running");

            while (true) {
                Socket socket = serverSocket.accept();

                clients.add(socket);
                System.out.println(socket.getInetAddress() + " is connected");

                // thread untuk menerima dan mengirim pesan ke semua clients
                new Thread(() -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String message;
                        while (!"!exit".equals(message = in.readLine())) { 
                            synchronized (clients) {
                                for (Socket client : clients) {
                                    if (!client.isClosed()) {
                                        try {
                                            PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                                            clientOut.println(message);
                                        } catch (IOException e) {
                                            System.err.println(e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println(e);
                    } finally {
                        synchronized (clients) {
                            clients.remove(socket);
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    }
                }).start();
            }
        }
    }
}