package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Masukkan server ip: ");
        String serverIP = userInput.readLine();

        System.out.print("Masukkan username: ");
        String username = userInput.readLine();

        try (Socket socket = new Socket(serverIP, 5000)) {
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);

            // thread untuk menerima pesan dari server
            new Thread(() -> {
                try {
                    String message;
                    while (!"!exit".equals(message = serverReader.readLine())) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }).start();

            // untuk mengirim pesan
            String message;
            while(!"!exit".equals(message = userInput.readLine())) {
                serverWriter.println(username + ": " + message);
            }
        }
    }
}
