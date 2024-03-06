package mockery;


import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class ConsoleLogger extends StreamHandler {
	static String formatLog(LogRecord lr) {
		return String.format("[%1$-7s] %2$s%n", lr.getLevel().getName(), lr.getMessage());
	}

	public static Logger createLogger(String name) {
		Logger logger = Logger.getLogger(name);
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);

		ConsoleHandler warning2stderrLogger = new ConsoleHandler();
		warning2stderrLogger.setLevel(Level.WARNING);
		warning2stderrLogger.setFormatter(new SimpleFormatter() {
			@Override
			public synchronized String format(LogRecord lr) {
				return formatLog(lr);
			}
		});
		logger.addHandler(warning2stderrLogger);

		ConsoleLogger fine2stdoutLogger = new ConsoleLogger();
		logger.addHandler(fine2stdoutLogger);

		return logger;
	}

	@Override
	public void publish(LogRecord record) {
		final Level level = record.getLevel();
		if (level.intValue() <= Level.INFO.intValue())
			System.out.print(formatLog(record));
	}
}
