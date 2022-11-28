package io.github.edsuns.nio.core;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import static io.github.edsuns.nio.util.ByteBufferUtil.popInt;
import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 0:47
 */
@ParametersAreNonnullByDefault
public abstract class QueuedProcessor implements NIOProcessor, Runnable {

    protected final ByteBufferPool byteBufferPool;
    protected State state;
    protected boolean mark;
    protected final ConcurrentLinkedDeque<ByteBuffer> readQueue;
    protected ConcurrentLinkedQueue<ByteBuffer> writeQueue;
    private int readLength = 0;
    private int readCount = 0;

    public QueuedProcessor(int bufferSize) {
        if (bufferSize <= 0) throw new IllegalArgumentException("bufferSize <= 0");
        this.byteBufferPool = new ByteBufferPool(bufferSize, 16);
        this.readQueue = new ConcurrentLinkedDeque<>();
        this.writeQueue = new ConcurrentLinkedQueue<>();
        this.state = State.READ;
        this.mark = true;
    }

    @Override
    public int initialKeyOps() {
        return state.getKeyOps();
    }

    @Override
    public State state() {
        return state;
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
    public State read(ByteBuffer readBuffer, ExecutorService executorService) {
        if (mark) {
            readLength = popInt(readBuffer);
            mark = false;
        }
        int read = readCount * readBuffer.capacity() - readBuffer.remaining();
        readQueue.offerLast(readBuffer);
        if (read < readLength) {
            return this.state = State.READ;
        } else {
            executorService.execute(this);
            return this.state = State.WRITE;
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
    public State wrote(@Nullable ByteBuffer writeBuffer) {
        if (writeBuffer == null) {
            return this.state;
        }
        if (writeBuffer.hasRemaining()) {
            return this.state = State.WRITE;
        } else {
            this.writeQueue.poll();
            return this.state = State.READ;
        }
    }

    @Override
    public void close() {
        this.state = State.CLOSE;
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
