package io.github.edsuns.nio.core;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.*;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 19:37
 */
@ParametersAreNonnullByDefault
public class Configuration {

    private int coreThreads = 2;
    private int maxThreads = Math.max(coreThreads, Runtime.getRuntime().availableProcessors());
    private int threadPoolQueueSize = 10;

    private int bufferSize = 2048;

    private Configuration() {
    }

    public static Configuration create() {
        return new Configuration();
    }

    public ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
                this.getCoreThreads(), this.getMaxThreads(),
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(this.getThreadPoolQueueSize()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    public int getCoreThreads() {
        return coreThreads;
    }

    public Configuration setCoreThreads(int coreThreads) {
        if (coreThreads < 2) {
            throw new IllegalArgumentException("coreThreads < 2");
        }
        this.coreThreads = coreThreads;
        return this;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public Configuration setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    public int getThreadPoolQueueSize() {
        return threadPoolQueueSize;
    }

    public Configuration setThreadPoolQueueSize(int threadPoolQueueSize) {
        this.threadPoolQueueSize = threadPoolQueueSize;
        return this;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Configuration setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }
}
