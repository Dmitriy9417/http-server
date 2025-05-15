package ru.netology;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final List<String> validPaths;

    public ConnectionHandler(Socket socket, List<String> validPaths) {
        this.socket = socket;
        this.validPaths = validPaths;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // Логика обработки запроса
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                out.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
                return;
            }

            String path = parts[1];
            if (!validPaths.contains(path)) {
                sendNotFound(out);
                return;
            }

            Path filePath = Path.of("public", path).normalize();
            if (!Files.exists(filePath)) {
                sendNotFound(out);
                return;
            }

            String mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                handleClassicHtml(filePath, mimeType, out);
            } else {
                sendFile(filePath, mimeType, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes());
    }

    private void handleClassicHtml(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        String template = Files.readString(filePath);
        byte[] content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
        String headers = "HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\nContent-Length: " + content.length + "\r\n\r\n";
        out.write(headers.getBytes());
        out.write(content);
    }

    private void sendFile(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        long length = Files.size(filePath);
        String headers = "HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\nContent-Length: " + length + "\r\n\r\n";
        out.write(headers.getBytes());
        Files.copy(filePath, out);
    }
}