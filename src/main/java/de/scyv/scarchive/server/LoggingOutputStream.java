package de.scyv.scarchive.server;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * Channels an output stream to a logger.
 */
public class LoggingOutputStream extends OutputStream {

    public static enum LogLevel {
        INFO, ERROR
    }

    /**
     * Default number of bytes in the buffer.
     */
    private static final int DEFAULT_BUFFER_LENGTH = 2048;

    /**
     * Indicates stream state.
     */
    private boolean hasBeenClosed = false;

    /**
     * Internal buffer where data is stored.
     */
    private byte[] buf;

    /**
     * The number of valid bytes in the buffer.
     */
    private int count;

    /**
     * Remembers the size of the buffer.
     */
    private int curBufLength;

    /**
     * The logger to write to.
     */
    private final Logger log;

    /**
     * The log level.
     */
    private final LogLevel level;

    /**
     * Creates the Logging instance to flush to the given logger.
     *
     * @param log
     *            the Logger to write to
     * @param level
     *            the log level
     * @throws IllegalArgumentException
     *             in case if one of arguments is null.
     */
    public LoggingOutputStream(final Logger log, final LogLevel level) throws IllegalArgumentException {
        if (log == null || level == null) {
            throw new IllegalArgumentException("Logger or log level must be not null");
        }
        this.log = log;
        this.level = level;
        curBufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[curBufLength];
        count = 0;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b
     *            the byte to write
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void write(final int b) throws IOException {
        if (hasBeenClosed) {
            throw new IOException("The stream has been closed.");
        }
        // don't log nulls
        if (b == 0) {
            return;
        }
        if (count == curBufLength) {
            // grow the buffer
            final int newBufLength = curBufLength + DEFAULT_BUFFER_LENGTH;
            final byte[] newBuf = new byte[newBufLength];
            System.arraycopy(buf, 0, newBuf, 0, curBufLength);
            buf = newBuf;
            curBufLength = newBufLength;
        }

        buf[count] = (byte) b;
        count++;
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be written
     * out.
     */
    @Override
    public void flush() {
        if (count == 0) {
            return;
        }
        final byte[] bytes = new byte[count];
        System.arraycopy(buf, 0, bytes, 0, count);
        final String str = new String(bytes);
        switch (this.level) {
        case ERROR:
            log.error(str);
            break;
        default:
            log.info(str);
            break;
        }
        count = 0;
    }

    /**
     * Closes this output stream and releases any system resources associated with
     * this stream.
     */
    @Override
    public void close() {
        flush();
        hasBeenClosed = true;
    }
}