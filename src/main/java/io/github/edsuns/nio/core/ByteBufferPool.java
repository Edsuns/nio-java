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

    public ByteBufferPool(int byteBufferCapacity) {
        this.byteBufferCapacity = byteBufferCapacity;
        this.pool =  new ConcurrentLinkedQueue<>();
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer byteBuffer = pool.poll();
        if (byteBuffer != null) {
            Profiler.INSTANCE.incrByteBufferPoolHits();
            return byteBuffer;
        } else {
            return ByteBuffer.allocate(byteBufferCapacity);
        }
    }

    public void recycle(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        pool.offer(byteBuffer);
    }
}
