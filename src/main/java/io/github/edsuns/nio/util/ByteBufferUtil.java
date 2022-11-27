package io.github.edsuns.nio.util;

import java.nio.ByteBuffer;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 2:05
 */
public final class ByteBufferUtil {

    public static ByteBuffer wrapWithLength(byte[] bytes) {
        byte[] wrap = new byte[bytes.length + 4];
        System.arraycopy(bytes, 0, wrap, 4, bytes.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(wrap);
        byteBuffer.putInt(0, bytes.length);
        byteBuffer.clear();
        return byteBuffer;
    }

    public static int popInt(ByteBuffer byteBuffer) {
        int x = byteBuffer.getInt(0);
        sliceLeft(byteBuffer, 4);
        return x;
    }

    public static void sliceLeft(ByteBuffer byteBuffer, int byteCount) {
        if (byteCount == 0) {
            return;
        }
        int offset = byteBuffer.arrayOffset();
        int cnt = byteBuffer.position() - offset - byteCount;
        if (cnt < 0) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(byteBuffer.array(), offset + byteCount, byteBuffer.array(), 0, cnt);
        byteBuffer.position(byteBuffer.position() - byteCount);
    }
}
