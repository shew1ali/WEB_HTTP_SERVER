package org.example;

import java.io.IOException;
public class Main {

    public static void main(String[] args) throws IOException {
        int poolSize = 64;
        int port = 9999;
        Server server = new Server(poolSize);
        server.start(port);
    }
}
