package io.github.edsuns.nio;

import io.github.edsuns.nio.client.NIOClient;
import io.github.edsuns.nio.core.Configuration;
import io.github.edsuns.nio.log.Log;

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
        Configuration configuration = Configuration.create();
        NIOClient client = new NIOClient(configuration);
        client.start(new InetSocketAddress("localhost", 80));
        log.info("server: %s", client.send(("hi").getBytes(StandardCharsets.UTF_8)).get().toString());

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String msg = client.send(scanner.nextLine().getBytes(StandardCharsets.UTF_8)).get().toString();
            log.info("server: %s", msg);
        }
    }
}
