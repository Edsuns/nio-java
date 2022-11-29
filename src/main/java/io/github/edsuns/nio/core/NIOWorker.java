package io.github.edsuns.nio.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.edsuns.nio.log.Log;

/**
 * @author edsuns@qq.com
 * @since 2022/11/26
 */
@ParametersAreNonnullByDefault
public class NIOWorker implements Runnable, Closeable {

    static final int STATE_NEW = 1;
    static final int STATE_BIND = 1 << 2;
    static final int STATE_CONNECTED = 1 << 3;
    static final int STATE_RUNNING = 1 << 4;
    static final int STATE_CLOSE = 1 << 5;
    static final int STATE_SHUTDOWN = 1 << 6;
    static final int STATE_CLOSED = 1 << 7;

    private static final Log log = Log.getLog(NIOWorker.class);

    private final ProcessorFactory processorFactory;
    private ExecutorService threadPool;

    @Nullable
    private Selector selector = null;
    private int state = STATE_NEW;

    public NIOWorker(ProcessorFactory processorFactory, int threads) {
        if (threads <= 0) throw new IllegalArgumentException("threads <= 0");
        this.processorFactory = processorFactory;
        this.threadPool = newFixedThreadPool(threads);
    }

    static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public synchronized void awaitConnection() throws IOException {
        if ((state & STATE_BIND) == 0) {
            throw new IllegalStateException("must bind() before awaitConnection()");
        }
        int cnt = 0;
        while ((state & STATE_CONNECTED) == 0) {
            try {
                wait(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            if (++cnt > 400) {
                throw new IOException("connection timeout");
            }
        }
    }

    @Override
    public void run() {
        if ((state & STATE_BIND) == 0) {
            throw new IllegalStateException("must bind() before run()");
        }

        Selector s;
        if ((state & STATE_SHUTDOWN) == 0 && (s = selector) != null) {
            SelectionKey key = null;
            try {
                s.select(1_000L);
                Iterator<SelectionKey> iter = s.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();

                    if (key.isAcceptable()) {
                        onAcceptable(key);
                    } else if (key.isConnectable()) {
                        onConnectable(key);
                    } else if (key.isReadable()) {
                        onReadable(key);
                    } else if (key.isWritable()) {
                        onWritable(key);
                    }

                    iter.remove();
                    key = null;
                }
            } catch (ClosedSelectorException cse) {
                // shutdown
            } catch (Throwable e) {
                log.debug("an error occurred in worker loop", e);
                cancelAndCloseKey(key);
            }
        }
        try {
            // run in loop
            threadPool.execute(this);
        } catch (RejectedExecutionException e) {
            // shutdown
        }
    }

    public synchronized NIOWorker bind(SocketAddress local, boolean serverOrClient) throws IOException {
        if ((state & STATE_BIND) != 0) {
            throw new IllegalStateException("already bound");
        }
        selector = Selector.open();
        if (serverOrClient) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(local);
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            state |= STATE_CONNECTED;
        } else {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.connect(local);
            clientChannel.register(selector, SelectionKey.OP_CONNECT);
        }
        state |= STATE_BIND;
        threadPool.execute(this);
        return this;
    }

    @Override
    public synchronized void close() throws IOException {
        if ((state & STATE_CLOSE) != 0 || selector == null) {
            return;
        }
        if ((state & STATE_BIND) == 0) {
            return;
        }
        state |= STATE_CLOSE;

        Selector _selector = selector;
        // wakeup selector to process its remaining work
        _selector.wakeup();
        // assign null to mark that it is shutting down
        selector = null;

        for (SelectionKey key : _selector.keys()) {
            cancelAndCloseKey(key);
        }
        _selector.close();
        state |= STATE_SHUTDOWN;

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60L, TimeUnit.SECONDS)) {
                throw new IOException("executorService termination timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        threadPool = null;

        state |= STATE_CLOSED;
        log.debug("NIOWorker closed");
    }

    protected void cancelAndCloseKey(@Nullable SelectionKey key) {
        if (key == null) {
            return;
        }
        try (SelectableChannel channel = key.channel()) {
            NIOProcessor processor = (NIOProcessor) key.attachment();
            if (processor != null) {
                processor.close();
            }
            key.cancel();
        } catch (IOException e) {
            log.debug("error occurred when cancelAndCloseKey()", e);
        }
    }

    protected void onAcceptable(SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        NIOProcessor processor = processorFactory.createProcessor();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, processor.initialKeyOps(), processor);
    }

    protected void onConnectable(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        clientChannel.finishConnect();
        NIOProcessor processor = processorFactory.createProcessor();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, processor.initialKeyOps(), processor);
        state |= STATE_CONNECTED;
    }

    protected void onWritable(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        NIOProcessor processor = (NIOProcessor) key.attachment();

        ByteBuffer writeBuffer = processor.writeBuffer();
        if (writeBuffer == null) {
            // TODO: replace with heartbeat message
            return;
        }
        int n = clientChannel.write(writeBuffer);
        if (n < 0) {
            cancelAndCloseKey(key);
        } else {
            // always call wrote to update the state even if zero bytes were written
            key.interestOps(processor.wrote(writeBuffer));
        }
    }

    protected void onReadable(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        NIOProcessor processor = (NIOProcessor) key.attachment();

        ByteBuffer readBuffer = processor.readBuffer();
        int n = clientChannel.read(readBuffer);
        if (n < 0) {
            cancelAndCloseKey(key);
        } else {
            // always call read to update the state even if zero bytes were read
            key.interestOps(processor.read(readBuffer, threadPool));
        }
    }

}
