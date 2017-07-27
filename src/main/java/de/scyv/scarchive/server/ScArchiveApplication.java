package de.scyv.scarchive.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScArchiveApplication {

	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(ScArchiveApplication.class);

	public static void main(String[] args) {

		// System.setErr(new PrintStream(new LoggingOutputStream(LOGGER,
		// LogLevel.ERROR)));
		// System.setOut(new PrintStream(new LoggingOutputStream(LOGGER,
		// LogLevel.INFO)));

		SpringApplication.run(ScArchiveApplication.class, args);

	}

}
