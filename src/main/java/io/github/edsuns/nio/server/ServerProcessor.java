package io.github.edsuns.nio.server;

import java.io.ByteArrayOutputStream;
import java.nio.channels.SelectionKey;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.core.QueuedProcessor;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 1:03
 */
@ParametersAreNonnullByDefault
public class ServerProcessor extends QueuedProcessor {

    private final Handler<ByteArrayOutputStream, byte[]> handler;

    public ServerProcessor(int bufferSize, Handler<ByteArrayOutputStream, byte[]> handler) {
        super(bufferSize, SelectionKey.OP_READ);
        this.handler = handler;
    }

    @Override
    protected void onMessage(ByteArrayOutputStream message) {
        this.reply(handler.onMessage(message));
    }

    @Override
    public void close() {
        // do nothing
    }
}
