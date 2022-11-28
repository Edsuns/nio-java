package io.github.edsuns.nio.server;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.core.NIOWorker;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 19:40
 */
@ParametersAreNonnullByDefault
public class NIOServer implements Closeable {

    private final NIOWorker worker;

    public NIOServer(int bufferSize, ExecutorService executorService, Handler<ByteArrayOutputStream, byte[]> handler) {
        this.worker = new NIOWorker(
                () -> new ServerProcessor(bufferSize, executorService, handler),
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
