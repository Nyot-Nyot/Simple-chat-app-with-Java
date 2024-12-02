package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            String serverIP = prompt(userInput, "Masukkan server IP: ");
            while (!isValidIP(serverIP)) {
                System.out.println("Invalid IP address. Please try again.");
                serverIP = prompt(userInput, "Masukkan server IP: ");
            }

            String username = prompt(userInput, "Masukkan username: ");
            while (username == null || username.trim().isEmpty()) {
                System.out.println("Username cannot be empty. Please try again.");
                username = prompt(userInput, "Masukkan username: ");
            }

            try (Socket socket = new Socket(serverIP, 5000);
                 PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true)) {

                Thread listenerThread = new Thread(new ServerListener(socket));
                listenerThread.start();

                // Send username to the server
                serverWriter.println(username);

                sendMessageLoop(userInput, serverWriter);

                serverWriter.println("!exit");
                listenerThread.join();

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

    private static String prompt(BufferedReader reader, String message) throws IOException {
        System.out.print(message);
        return reader.readLine();
    }

    private static void sendMessageLoop(BufferedReader userInput, PrintWriter serverWriter) throws IOException {
        String message;
        while (!"!exit".equals(message = userInput.readLine())) {
            serverWriter.println(message);
        }
    }

    private static boolean isValidIP(String ip) {
        // Simple IP address validation
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    private static class ServerListener implements Runnable {
        private final Socket socket;

        public ServerListener(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = serverReader.readLine()) != null && !"!exit".equals(message)) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
            } finally {
                closeSocket();
            }
        }

        private void closeSocket() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
