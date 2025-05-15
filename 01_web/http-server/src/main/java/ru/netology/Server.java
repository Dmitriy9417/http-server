package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService executorService;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.executorService = Executors.newFixedThreadPool(64); // Пул на 64 потока
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                executorService.submit(new ConnectionHandler(socket, validPaths));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown(); // Завершаем пул при закрытии сервера
        }
    }
}
