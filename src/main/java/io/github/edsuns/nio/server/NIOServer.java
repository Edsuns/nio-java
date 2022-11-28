package io.github.edsuns.nio.server;

import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.core.NIOWorker;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 19:40
 */
@ParametersAreNonnullByDefault
public class NIOServer implements Closeable {

    private final NIOWorker worker;

    public NIOServer(int bufferSize, int threads, Handler<ByteArrayOutputStream, byte[]> handler) {
        this.worker = new NIOWorker(
                () -> new ServerProcessor(bufferSize, handler),
                threads
        );
    }

    public void start(SocketAddress local) throws IOException {
        worker.bind(local, true);
    }

    @Override
    public void close() throws IOException {
        worker.close();
    }
}
