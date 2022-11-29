package io.github.edsuns.nio.client;

import java.io.ByteArrayOutputStream;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.edsuns.nio.core.QueuedProcessor;

import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 4:35
 */
@ParametersAreNonnullByDefault
public class ClientProcessor extends QueuedProcessor {

    private final ConcurrentLinkedQueue<CompletableFuture<ByteArrayOutputStream>> callbackQueue;

    public ClientProcessor(int bufferSize) {
        super(bufferSize, SelectionKey.OP_WRITE);
        this.callbackQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    protected void onMessage(ByteArrayOutputStream message) {
        requireNonNull(callbackQueue.poll()).complete(message);
    }

    public CompletableFuture<ByteArrayOutputStream> send(byte[] message) {
        CompletableFuture<ByteArrayOutputStream> future = new CompletableFuture<>();
        this.callbackQueue.offer(future);
        this.reply(message);
        return future;
    }

    @Override
    public void close() {
        shutdown();
    }

    private synchronized void shutdown() {
        for (int i = 0; i < 3; i++) {
            CompletableFuture<?> future;
            while ((future = callbackQueue.poll()) != null) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else if (cause instanceof Error) {
                        throw (Error) cause;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                wait(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
