package io.github.edsuns.nio.client;

import io.github.edsuns.nio.core.QueuedProcessor;
import io.github.edsuns.nio.core.State;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static io.github.edsuns.nio.util.ByteBufferUtil.wrapWithLength;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 4:35
 */
@ParametersAreNonnullByDefault
public class ClientProcessor extends QueuedProcessor {

    private final ConcurrentLinkedQueue<Consumer<ByteArrayOutputStream>> callbackQueue;

    public ClientProcessor(int bufferSize, ExecutorService executorService) {
        super(bufferSize, executorService);
        this.callbackQueue = new ConcurrentLinkedQueue<>();
        this.state = State.WRITE;
        this.mark = false;
    }

    @Override
    protected synchronized void onMessage(ByteArrayOutputStream message) {
        Consumer<ByteArrayOutputStream> callback = callbackQueue.poll();
        if (callback != null) {
            callback.accept(message);
        }
    }

    public Future<ByteArrayOutputStream> send(byte[] message) {
        ConsumerFuture<ByteArrayOutputStream> future = new ConsumerFuture<>();
        this.callbackQueue.offer(future);
        ByteBuffer msg = wrapWithLength(message);
        this.writeQueue.offer(msg);
        return future;
    }

    static class ConsumerFuture<T> extends FutureTask<T> implements Consumer<T> {
        static final Callable<?> callable = () -> null;

        @SuppressWarnings("unchecked")
        public ConsumerFuture() {
            super((Callable<T>) callable);
        }

        @Override
        public void run() {
        }

        @Override
        public void accept(T message) {
            set(message);
        }
    }

}
