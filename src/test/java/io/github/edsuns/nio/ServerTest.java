package io.github.edsuns.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.Executors;

import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.log.Log;
import io.github.edsuns.nio.log.Profiler;
import io.github.edsuns.nio.server.NIOServer;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 5:48
 */
public class ServerTest {
    static {
        Log.setLevel(Log.LEVEL_DEBUG);
    }

    private static final Log log = Log.getLog(ServerTest.class);

    public static void main(String[] args) throws IOException {
        Handler<ByteArrayOutputStream, byte[]> handler = msg -> {
            log.info("client: %s", msg);
            return msg.toByteArray();
        };
        int port = 8082;
        NIOServer server = new NIOServer(2048, Executors.newFixedThreadPool(2), handler);
        server.start(new InetSocketAddress("localhost", port));
        log.info("server started at port: %s", port);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("prof".equals(line)) {
                Profiler.INSTANCE.printReport();
            }
        }
    }
}
