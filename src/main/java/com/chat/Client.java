package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("Masukkan server ip: ");
            String serverIP = userInput.readLine();

            System.out.print("Masukkan username: ");
            String username = userInput.readLine();

            try (Socket socket = new Socket(serverIP, 5000);
                 BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true)) {

                Thread receiveThread = new Thread(() -> {
                    try {
                        String message;
                        while ((message = serverReader.readLine()) != null && !"!exit".equals(message)) {
                            if (!message.startsWith(socket.getInetAddress().toString())) {
                                System.out.println(message);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                });
                receiveThread.start();

                String message;
                while (!"!exit".equals(message = userInput.readLine())) {
                    serverWriter.println(username + ": " + message);
                }

                // Signal the server about client exit
                serverWriter.println("!exit");
                receiveThread.join();

            } catch (IOException e) {
                System.err.println("Error connecting to server: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Client thread interrupted: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Error reading user input: " + e.getMessage());
        }
    }
}
