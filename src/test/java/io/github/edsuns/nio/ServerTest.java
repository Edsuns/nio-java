package io.github.edsuns.nio;

import io.github.edsuns.nio.core.Configuration;
import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.log.Log;
import io.github.edsuns.nio.server.NIOServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

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
        Configuration configuration = Configuration.create();
        Handler<ByteArrayOutputStream, byte[]> handler = msg -> {
            log.info("client: %s", msg);
            return msg.toByteArray();
        };
        int port = 80;
        NIOServer server = new NIOServer(configuration, handler);
        server.start(new InetSocketAddress("localhost", port));
        log.info("server started at port: %s", port);
    }
}
