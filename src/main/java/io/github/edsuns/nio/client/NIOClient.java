package io.github.edsuns.nio.client;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.edsuns.nio.core.NIOProcessor;
import io.github.edsuns.nio.core.NIOWorker;
import io.github.edsuns.nio.core.ProcessorFactory;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 20:03
 */
@ParametersAreNonnullByDefault
public class NIOClient implements Closeable {

    private final NIOWorker worker;
    private final ClientProcessor clientProcessor;

    private boolean closed = false;

    public NIOClient(int bufferSize, ExecutorService executorService) {
        SingleClientProcessorFactory factory =
                new SingleClientProcessorFactory(bufferSize, executorService);
        this.clientProcessor = factory.getClientProcessor();
        this.worker = new NIOWorker(factory, executorService);
    }

    public void start(SocketAddress local) throws IOException {
        try {
            worker.bind(local, false);
        } catch (IOException e) {
            close();
            throw e;
        }
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

    static class SingleClientProcessorFactory implements ProcessorFactory {
        private final ClientProcessor clientProcessor;
        public SingleClientProcessorFactory(int bufferSize, ExecutorService executorService) {
            this.clientProcessor = new ClientProcessor(bufferSize, executorService);
        }
        public ClientProcessor getClientProcessor() { return clientProcessor; }
        @Override
        public NIOProcessor createProcessor() { return clientProcessor; }
    }
}
