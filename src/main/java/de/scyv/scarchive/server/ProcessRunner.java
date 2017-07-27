package de.scyv.scarchive.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.scyv.scarchive.server.LoggingOutputStream.LogLevel;

/**
 * Runs a process and waits until it finishes.
 *
 */
public abstract class ProcessRunner {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private String[] command;

	public void setCommand(String[] command) {
		this.command = command;
	}

	public int run() throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(command);
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
		private final OutputStream ostrm;
		private final InputStream istrm;

		public SyncPipe(InputStream istrm, OutputStream ostrm) {
			this.istrm = istrm;
			this.ostrm = ostrm;
		}

		public void run() {
			try {
				final byte[] buffer = new byte[1024];
				for (int length = 0; (length = istrm.read(buffer)) != -1;) {
					ostrm.write(buffer, 0, length);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
