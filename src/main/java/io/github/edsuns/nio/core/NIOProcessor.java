package io.github.edsuns.nio.core;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.intellij.lang.annotations.MagicConstant;

/**
 * @author edsuns@qq.com
 * @since 2022/11/26
 */
@ParametersAreNonnullByDefault
public interface NIOProcessor extends Closeable {

    @MagicConstant(flagsFromClass = SelectionKey.class)
    int initialKeyOps();

    ByteBuffer readBuffer();

    @MagicConstant(flagsFromClass = SelectionKey.class)
    int read(ByteBuffer readBuffer, ExecutorService executorService);

    @Nullable
    ByteBuffer writeBuffer();

    @MagicConstant(flagsFromClass = SelectionKey.class)
    int wrote(ByteBuffer writeBuffer);

    void reply(byte[] message);

    void close();
}
