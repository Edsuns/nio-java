package io.github.edsuns.nio.client;

import io.github.edsuns.nio.core.NIOProcessor;
import io.github.edsuns.nio.core.NIOWorker;
import io.github.edsuns.nio.core.ProcessorFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Future;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 20:03
 */
@ParametersAreNonnullByDefault
public class NIOClient implements Closeable {

    private final NIOWorker worker;
    private final ClientProcessor clientProcessor;

    private boolean closed = false;

    public NIOClient(int bufferSize, int threads) {
        this.clientProcessor = new ClientProcessor(bufferSize);
        this.worker = new NIOWorker(new ClientProcessorFactory(clientProcessor), threads);
    }

    public void start(SocketAddress local) throws IOException {
        worker.bind(local, false);
    }

    public Future<ByteArrayOutputStream> send(byte[] message) {
        if (closed) throw new IllegalStateException("closed");
        return clientProcessor.send(message);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        worker.close();
        try {
            wait(100L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class ClientProcessorFactory implements ProcessorFactory {
        private final ClientProcessor clientProcessor;
        public ClientProcessorFactory(ClientProcessor clientProcessor) {
            this.clientProcessor = clientProcessor;
        }
        @Override
        public NIOProcessor createProcessor() { return clientProcessor; }
    }
}
