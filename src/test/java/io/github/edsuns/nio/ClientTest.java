package io.github.edsuns.nio;

import io.github.edsuns.nio.client.NIOClient;
import io.github.edsuns.nio.log.Log;
import io.github.edsuns.nio.log.Profiler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 5:43
 */
public class ClientTest {
    static {
        Log.setLevel(Log.LEVEL_DEBUG);
    }

    private final static Log log = Log.getLog(ClientTest.class);

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        NIOClient client = new NIOClient(2048, 2);
        client.start(new InetSocketAddress("localhost", 8082));
        log.info("server: %s", client.send(("hi").getBytes(StandardCharsets.UTF_8)).get().toString());

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("prof".equals(line)) {
                Profiler.INSTANCE.printReport();
            } else {
                String msg = client.send(line.getBytes(StandardCharsets.UTF_8)).get().toString();
                log.info("server: %s", msg);
            }
        }
    }
}
