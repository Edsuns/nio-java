package io.github.edsuns.nio.server;

import io.github.edsuns.nio.core.Handler;
import io.github.edsuns.nio.core.QueuedProcessor;
import io.github.edsuns.nio.core.State;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;

import static io.github.edsuns.nio.util.ByteBufferUtil.wrapWithLength;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 1:03
 */
@ParametersAreNonnullByDefault
public class ServerProcessor extends QueuedProcessor {

    private final Handler<ByteArrayOutputStream, byte[]> handler;

    public ServerProcessor(int bufferSize, Handler<ByteArrayOutputStream, byte[]> handler) {
        super(bufferSize);
        this.handler = handler;
        this.state = State.READ;
        this.mark = true;
    }

    @Override
    protected void onMessage(ByteArrayOutputStream message) {
        this.writeQueue.offer(wrapWithLength(handler.onMessage(message)));
    }
}
