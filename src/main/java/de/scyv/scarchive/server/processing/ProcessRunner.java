package de.scyv.scarchive.server.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.scyv.scarchive.server.LoggingOutputStream;
import de.scyv.scarchive.server.LoggingOutputStream.LogLevel;

/**
 * Runs a process and waits until it finishes.
 *
 * STDERR and STDOUT are channelled to the logging facility.
 */
public abstract class ProcessRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String[] command;

    public void setCommand(String[] command) {
        this.command = command;
    }

    /**
     * Run the process and wait until finished (Blocking call!).
     *
     * @return the exit code of the command.
     * @throws IOException
     *             when the command cannot be executed (perhaps cannot be found)
     * @throws InterruptedException
     *             when the command is interrupted unexpectedly
     */
    public int run() throws IOException, InterruptedException {
        final Process p = Runtime.getRuntime().exec(command);
        new Thread(new SyncPipe(p.getErrorStream(), new PrintStream(new LoggingOutputStream(logger, LogLevel.ERROR))))
                .start();
        new Thread(new SyncPipe(p.getInputStream(), new PrintStream(new LoggingOutputStream(logger, LogLevel.INFO))))
                .start();
        return p.waitFor();
    }

    /**
     * Syncs an OutputStream with an InputStream.
     *
     * From: https://stackoverflow.com/a/5437863/4561247
     */
    private static final class SyncPipe implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(SyncPipe.class);
        private final OutputStream ostrm;
        private final InputStream istrm;

        /**
         * Create an instance.
         *
         * @param istrm
         *            the inputstream to read from.
         * @param ostrm
         *            the outputstrem to write to.
         */
        public SyncPipe(InputStream istrm, OutputStream ostrm) {
            this.istrm = istrm;
            this.ostrm = ostrm;
        }

        @Override
        public void run() {
            try {
                final byte[] buffer = new byte[1024];
                for (int length = 0; (length = istrm.read(buffer)) != -1;) {
                    ostrm.write(buffer, 0, length);
                }
            } catch (final IOException ex) {
                LOGGER.error("Cannot write to stream", ex);
            }
        }
    }
}
