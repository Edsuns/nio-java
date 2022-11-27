package io.github.edsuns.nio.core;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 1:07
 */
@FunctionalInterface
public interface Handler<MSG, T> {

    T onMessage(MSG message);
}
