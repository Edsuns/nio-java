package io.github.edsuns.nio.core;

import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.channels.SelectionKey;

import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/26
 */
@ParametersAreNonnullByDefault
public enum State {
    READ(SelectionKey.OP_READ), WRITE(SelectionKey.OP_WRITE), CLOSE(null);

    @Nullable
    @MagicConstant(flagsFromClass = SelectionKey.class)
    private final Integer keyOps;

    State(@Nullable @MagicConstant(flagsFromClass = SelectionKey.class) Integer keyOps) {
        this.keyOps = keyOps;
    }

    @MagicConstant(flagsFromClass = SelectionKey.class)
    public int getKeyOps() {
        return requireNonNull(keyOps, () -> this + " does not support keyOps");
    }
}
