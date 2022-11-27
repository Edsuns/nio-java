package io.github.edsuns.nio.core;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author edsuns@qq.com
 * @since 2022/11/26
 */
@FunctionalInterface
@ParametersAreNonnullByDefault
public interface ProcessorFactory {

    /**
     * create a customize {@link NIOProcessor}
     *
     * @return {@link NIOProcessor}
     */
    NIOProcessor createProcessor();
}
