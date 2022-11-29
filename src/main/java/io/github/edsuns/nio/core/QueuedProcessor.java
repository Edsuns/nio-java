package io.github.edsuns.nio.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static io.github.edsuns.nio.util.ByteBufferUtil.popInt;
import static io.github.edsuns.nio.util.ByteBufferUtil.wrapWithLength;
import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 0:47
 */
@ParametersAreNonnullByDefault
public abstract class QueuedProcessor implements NIOProcessor, Runnable {

    protected final ByteBufferPool byteBufferPool;
    protected final int initialKeyOps;
    private boolean mark;
    private final ConcurrentLinkedDeque<ByteBuffer> readQueue;
    private final ConcurrentLinkedQueue<ByteBuffer> writeQueue;
    private int readLength = 0;
    private int readCount = 0;

    public QueuedProcessor(int bufferSize, int initialKeyOps) {
        if (bufferSize <= 0) throw new IllegalArgumentException("bufferSize <= 0");
        this.byteBufferPool = new ByteBufferPool(bufferSize, 16);
        this.readQueue = new ConcurrentLinkedDeque<>();
        this.writeQueue = new ConcurrentLinkedQueue<>();
        this.initialKeyOps = initialKeyOps;
        this.mark = initialKeyOps == SelectionKey.OP_READ;
    }

    @Override
    public int initialKeyOps() {
        return initialKeyOps;
    }

    @Nullable
    @Override
    public ByteBuffer readBuffer() {
        ByteBuffer readBuffer = readQueue.peekLast();
        if (readBuffer != null && readBuffer.hasRemaining()) {
            return requireNonNull(readQueue.pollLast());
        } else {
            readCount++;
            return byteBufferPool.getByteBuffer();
        }
    }

    @Override
    public int read(ByteBuffer readBuffer, ExecutorService executorService) {
        if (mark) {
            readLength = popInt(readBuffer);
            mark = false;
        }
        int read = readCount * readBuffer.capacity() - readBuffer.remaining();
        readQueue.offerLast(readBuffer);
        if (read < readLength) {
            return SelectionKey.OP_READ;
        } else {
            executorService.execute(this);
            return SelectionKey.OP_WRITE;
        }
    }

    @Nullable
    @Override
    public ByteBuffer writeBuffer() {
        if (!mark) {
            mark = true;
        }
        return this.writeQueue.peek();
    }

    @Override
    public int wrote(ByteBuffer writeBuffer) {
        if (writeBuffer.hasRemaining()) {
            return SelectionKey.OP_WRITE;
        } else {
            this.writeQueue.poll();
            return SelectionKey.OP_READ;
        }
    }

    @Override
    public void reply(byte[] message) {
        this.writeQueue.offer(wrapWithLength(message));
    }

    @Override
    public synchronized void run() {
        onMessage(packMessageFromReadQueue());
    }

    protected abstract void onMessage(ByteArrayOutputStream message);

    protected ByteArrayOutputStream packMessageFromReadQueue() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(readLength);
        int n = 0, cnt;
        ByteBuffer byteBuffer;
        while ((byteBuffer = readQueue.poll()) != null) {
            byteBuffer.flip();
            cnt = Math.min(byteBuffer.limit(), readLength - n);
            out.write(byteBuffer.array(), 0, cnt);
            byteBuffer.position(byteBuffer.position() + cnt);
            n += cnt;

            if (byteBuffer.hasRemaining()) {
                System.arraycopy(byteBuffer.array(), byteBuffer.position(), byteBuffer.array(), 0, byteBuffer.remaining());
                byteBuffer.position(byteBuffer.remaining());
                byteBuffer.limit(byteBuffer.capacity());
                readQueue.offerFirst(byteBuffer);
                break;
            } else {
                byteBufferPool.recycle(byteBuffer);
            }
        }
        // reset
        readLength = 0;
        readCount = 0;
        return out;
    }

}
