package io.github.edsuns.nio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.github.edsuns.nio.log.Profiler;

/**
 * @author edsuns@qq.com
 * @since 2022/11/28 10:09
 */
public class ByteBufferPool {

    protected final int byteBufferCapacity;
    protected final ConcurrentLinkedQueue<ByteBuffer> pool;
    protected final int maxSize;
    private int size;

    public ByteBufferPool(int byteBufferCapacity, int maxSize) {
        this.byteBufferCapacity = byteBufferCapacity;
        this.maxSize = maxSize;
        this.size = 0;
        this.pool =  new ConcurrentLinkedQueue<>();
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer byteBuffer = pool.poll();
        if (byteBuffer == null) {
            // only one thread will modify size, so there is no concurrent problems
            size++;
            byteBuffer = ByteBuffer.allocate(byteBufferCapacity);
        } else {
            Profiler.INSTANCE.incrByteBufferPoolHits();
        }
        // trim to maxSize to prevent memory leak
        while (size > maxSize && pool.poll() != null) {
            size--;
        }
        return byteBuffer;
    }

    public void recycle(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        pool.offer(byteBuffer);
    }
}
