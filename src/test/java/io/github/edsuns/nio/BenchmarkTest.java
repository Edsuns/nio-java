package io.github.edsuns.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.edsuns.nio.client.NIOClient;
import io.github.edsuns.nio.log.Profiler;
import io.github.edsuns.nio.server.NIOServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zhongjiawei@joyy.com
 * @since 2022/11/28 14:01
 */
public class BenchmarkTest {

    @Test
    void benchmark() throws IOException, InterruptedException, ExecutionException {
        InetSocketAddress localhost = new InetSocketAddress("localhost", 9080);
        int bufferSize = 1024 * 16;
        int messageSize = bufferSize * 2;
        byte[] message = generateMessage(messageSize);
        int threads = 4, opPerThread = 1000;

        NIOServer server = new NIOServer(bufferSize, Executors.newFixedThreadPool(4), ByteArrayOutputStream::toByteArray);
        server.start(localhost);

        List<NIOClient> clients = new LinkedList<>();
        for (int i = 0; i < threads; i++) {
            NIOClient client = new NIOClient(bufferSize, Executors.newFixedThreadPool(1));
            client.start(localhost);
            clients.add(client);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        List<Future<?>> tasks = new LinkedList<>();

        long start = System.currentTimeMillis();
        for (NIOClient client : clients) {
            Future<Object> task = threadPool.submit(() -> {
                List<Future<ByteArrayOutputStream>> replies = new LinkedList<>();
                for (int j = 0; j < opPerThread; j++) {
                    replies.add(client.send(message));
                }
                for (Future<ByteArrayOutputStream> reply : replies) {
                    assertArrayEquals(message, reply.get().toByteArray());
                }
                return null;
            });
            tasks.add(task);
        }
        for (Future<?> task : tasks) {
            task.get();
        }
        long timeElapsed = System.currentTimeMillis() - start;

        threadPool.shutdown();
        assertTrue(threadPool.awaitTermination(20, TimeUnit.SECONDS));

        // there are upload and download, so '*2'
        System.out.printf("%s*2 MB/s\n", (double) (opPerThread * threads * messageSize) / timeElapsed / 1024 / 1024 * 1000);
        Profiler.INSTANCE.printReport();
    }

    private byte[] generateMessage(int size) {
        byte[] msg = new byte[size];
        for (int i = 0; i < size; i++) {
            msg[i] = (byte) (Math.random() * Byte.MAX_VALUE);
        }
        return msg;
    }
}
