package io.github.edsuns.nio.server;

import io.github.edsuns.nio.core.Configuration;
import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.core.NIOWorker;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 19:40
 */
@ParametersAreNonnullByDefault
public class NIOServer implements Closeable {

    private final ExecutorService executorService;
    private final NIOWorker worker;

    public NIOServer(Configuration configuration, Handler<ByteArrayOutputStream, byte[]> handler) {
        this.executorService = configuration.createExecutorService();
        this.worker = new NIOWorker(
                () -> new ServerProcessor(configuration.getBufferSize(), executorService, handler),
                executorService
        );
    }

    public void start(SocketAddress local) throws IOException {
        try {
            worker.bind(local, true);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        worker.close();
    }
}
