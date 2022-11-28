package io.github.edsuns.nio.log;

/**
 * @author edsuns@qq.com
 * @since 2022/11/28 12:49
 */
public class Profiler {

    public static final Profiler INSTANCE = new Profiler();

    private int byteBufferPoolHits;

    public void incrByteBufferPoolHits() {
        byteBufferPoolHits++;
    }

    public void printReport() {
        StringBuilder builder = new StringBuilder("\n[Profiler Report]\n");
        builder.append("byteBufferPoolHits=").append(byteBufferPoolHits).append("\n");
        System.out.println(builder);
    }
}
