package io.github.edsuns.nio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 4:30
 */
public final class ByteArrayOutputStreamUtil {

    public static ByteBuffer toByteBuffer(ByteArrayOutputStream out) {
        Object[] objects = hookBuf(out);
        byte[] buf = (byte[]) objects[0];
        int length = (int) objects[1];
        return ByteBuffer.wrap(buf, 0, length);
    }

    public static ByteArrayInputStream toInputStream(ByteArrayOutputStream out) {
        Object[] objects = hookBuf(out);
        byte[] buf = (byte[]) objects[0];
        int length = (int) objects[1];
        return new ByteArrayInputStream(buf, 0, length);
    }

    private static Object[] hookBuf(ByteArrayOutputStream out) {
        final byte[][] buff = {null};
        final int[] length = {0};
        try {
            out.writeTo(new ByteArrayOutputStream(0) {
                @Override
                public synchronized void write(byte[] b, int off, int len) {
                    buff[0] = b;
                    length[0] = len;
                }
            });
        } catch (IOException e) {
            throw new InternalError(e);
        }
        return new Object[]{buff[0], length[0]};
    }
}
