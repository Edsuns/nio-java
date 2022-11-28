package io.github.edsuns.nio.client;

import io.github.edsuns.nio.core.QueuedProcessor;
import io.github.edsuns.nio.core.State;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.github.edsuns.nio.util.ByteBufferUtil.wrapWithLength;
import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 4:35
 */
@ParametersAreNonnullByDefault
public class ClientProcessor extends QueuedProcessor {

    private final ConcurrentLinkedQueue<CompletableFuture<ByteArrayOutputStream>> callbackQueue;

    public ClientProcessor(int bufferSize) {
        super(bufferSize);
        this.callbackQueue = new ConcurrentLinkedQueue<>();
        this.state = State.WRITE;
        this.mark = false;
    }

    @Override
    protected void onMessage(ByteArrayOutputStream message) {
        requireNonNull(callbackQueue.poll()).complete(message);
    }

    public CompletableFuture<ByteArrayOutputStream> send(byte[] message) {
        CompletableFuture<ByteArrayOutputStream> future = new CompletableFuture<>();
        this.callbackQueue.offer(future);
        ByteBuffer msg = wrapWithLength(message);
        this.writeQueue.offer(msg);
        return future;
    }

}
