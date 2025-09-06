package com.bluejeans.bigqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

class MappedPage implements Closeable {

    private final static Logger logger = LoggerFactory.getLogger(MappedPage.class);

    private ThreadLocalByteBuffer threadLocalBuffer;
    private volatile boolean dirty = false;
    private volatile boolean closed = false;
    private final String pageFile;
    private final long index;

    public MappedPage(final MappedByteBuffer mbb, final String pageFile, final long index) {
        this.threadLocalBuffer = new ThreadLocalByteBuffer(mbb);
        this.pageFile = pageFile;
        this.index = index;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed)
                return;

            flush();

            final MappedByteBuffer srcBuf = (MappedByteBuffer) threadLocalBuffer.getSourceBuffer();
            unmap(srcBuf);

            this.threadLocalBuffer = null; // hint GC

            closed = true;
            if (logger.isDebugEnabled())
                logger.debug("Mapped page for " + this.pageFile + " was just unmapped and closed.");
        }
    }

    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Persist any changes to disk
     */

    public void flush() {
        synchronized (this) {
            if (closed)
                return;
            if (dirty) {
                final MappedByteBuffer srcBuf = (MappedByteBuffer) threadLocalBuffer.getSourceBuffer();
                srcBuf.force(); // flush the changes
                dirty = false;
                if (logger.isDebugEnabled())
                    logger.debug("Mapped page for " + this.pageFile + " was just flushed.");
            }
        }
    }

    /**
     * Get data from a thread local copy of the mapped page buffer
     *
     * @param position start position(relative to the start position of source mapped page buffer) of the thread local buffer
     * @param length the length to fetch
     * @return byte data
     */

    public byte[] getLocal(final int position, final int length) {
        final ByteBuffer buf = this.getLocal(position);
        final byte[] data = new byte[length];
        buf.get(data);
        return data;
    }

    /**
     * Get a thread local copy of the mapped page buffer
     *
     * @param position start position(relative to the start position of source mapped page buffer) of the thread local buffer
     * @return a byte buffer with specific position as start position.
     */

    public ByteBuffer getLocal(final int position) {
        final ByteBuffer buf = this.threadLocalBuffer.get();
        buf.position(position);
        return buf;
    }

    private static void unmap(final MappedByteBuffer buffer) {
        Cleaner.clean(buffer);
    }

    /**
     * Helper class allowing to clean direct buffers.
     */
    private static class Cleaner {
        public static final boolean CLEAN_SUPPORTED;
        private static final Method directBufferCleaner;
        private static final Method directBufferCleanerClean;

        static {
            Method directBufferCleanerX = null;
            Method directBufferCleanerCleanX = null;
            boolean v;
            try {
                directBufferCleanerX = Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner");
                directBufferCleanerX.setAccessible(true);
//                directBufferCleanerCleanX = Class.forName("sun.misc.Cleaner").getMethod("clean");
                directBufferCleanerCleanX = Class.forName("jdk.internal.ref.Cleaner").getMethod("clean");
                directBufferCleanerCleanX.setAccessible(true);
                v = true;
            }
            catch (final Exception e) {
                v = false;
            }
            CLEAN_SUPPORTED = v;
            directBufferCleaner = directBufferCleanerX;
            directBufferCleanerClean = directBufferCleanerCleanX;
        }

        public static void clean(final ByteBuffer buffer) {
            if (buffer == null)
                return;
            if (CLEAN_SUPPORTED && buffer.isDirect())
                try {
                    final Object cleaner = directBufferCleaner.invoke(buffer);
                    directBufferCleanerClean.invoke(cleaner);
                }
                catch (final Exception e) {
                    // silently ignore exception
                }
        }
    }

    private static class ThreadLocalByteBuffer extends ThreadLocal<ByteBuffer> {
        private final ByteBuffer _src;

        public ThreadLocalByteBuffer(final ByteBuffer src) {
            _src = src;
        }

        public ByteBuffer getSourceBuffer() {
            return _src;
        }

        @Override
        protected synchronized ByteBuffer initialValue() {
            final ByteBuffer dup = _src.duplicate();
            return dup;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "Mapped page for " + this.pageFile + ", index = " + this.index + ".";
    }

    public String getPageFile() {
        return this.pageFile;
    }

    /**
     * The index of the mapped page
     *
     * @return the index
     */

    public long getPageIndex() {
        return this.index;
    }
}
