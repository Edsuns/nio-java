package io.github.edsuns.nio.core;

import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;

/**
 * @author edsuns@qq.com
 * @since 2022/11/26
 */
@ParametersAreNonnullByDefault
public interface NIOProcessor extends Closeable {

    @MagicConstant(flagsFromClass = SelectionKey.class)
    int initialKeyOps();

    State state();

    ByteBuffer readBuffer();

    State read(ByteBuffer readBuffer, ExecutorService executorService);

    @Nullable
    ByteBuffer writeBuffer();

    State wrote(@Nullable ByteBuffer writeBuffer);

    void close();
}
