package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    ExecutorService executorService;

    public Server(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public void start(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> connect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);


            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public class Main {
        public static final String GET = "GET";
        public static final String POST = "POST";

        public static void main(String[] args) {
            final var allowedMethods = List.of(GET, POST);

            try (final var serverSocket = new ServerSocket(9999)) {
                while (true) {
                    try (
                            final var socket = serverSocket.accept();
                            final var in = new BufferedInputStream(socket.getInputStream());
                            final var out = new BufferedOutputStream(socket.getOutputStream());
                    ) {

                        final var limit = 4096;

                        in.mark(limit);
                        final var buffer = new byte[limit];
                        final var read = in.read(buffer);

                        final var requestLineDelimiter = new byte[]{'\r', '\n'};
                        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                        if (requestLineEnd == -1) {
                            badRequest(out);
                            continue;
                        }

                        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                        if (requestLine.length != 3) {
                            badRequest(out);
                            continue;
                        }

                        final var method = requestLine[0];
                        if (!allowedMethods.contains(method)) {
                            badRequest(out);
                            continue;
                        }
                        System.out.println(method);

                        final var path = requestLine[1];
                        if (!path.startsWith("/")) {
                            badRequest(out);
                            continue;
                        }
                        System.out.println(path);


                        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                        final var headersStart = requestLineEnd + requestLineDelimiter.length;
                        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                        if (headersEnd == -1) {
                            badRequest(out);
                            continue;
                        }


                        in.reset();
                        in.skip(headersStart);

                        final var headersBytes = in.readNBytes(headersEnd - headersStart);
                        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                        System.out.println(headers);

                        // для GET тела нет
                        if (!method.equals(GET)) {
                            in.skip(headersDelimiter.length);
                            final var contentLength = extractHeader(headers, "Content-Length");
                            if (contentLength.isPresent()) {
                                final var length = Integer.parseInt(contentLength.get());
                                final var bodyBytes = in.readNBytes(length);

                                final var body = new String(bodyBytes);
                                System.out.println(body);
                            }
                        }

                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static Optional<String> extractHeader(List<String> headers, String header) {
            return headers.stream()
                    .filter(o -> o.startsWith(header))
                    .map(o -> o.substring(o.indexOf(" ")))
                    .map(String::trim)
                    .findFirst();
        }

        private static void badRequest(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        private static int indexOf(byte[] array, byte[] target, int start, int max) {
            outer:
            for (int i = start; i < max - target.length + 1; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (array[i + j] != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
    }
}