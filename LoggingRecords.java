import java.util.logging.Formatter;
import java.util.logging.*;
import java.io.IOException;
import java.util.Date;

public class LoggingRecords {

    Logger logUtil;
    FileHandler fileHandler;

    LoggingRecords(String pID) throws IOException {
        logUtil = Logger.getLogger(pID);
        String pattern = "./" + pID + "/logs_" + pID + ".log";
        fileHandler = new FileHandler(pattern);
        fileHandler.setFormatter(new LogFormatter());
        logUtil.addHandler(fileHandler);

    }

    public void logInfo(String nws) {
        logUtil.log(new LogRecord(Level.INFO, nws));
    }


    class LogFormatter extends Formatter {

        @Override
        public String format(LogRecord logRecord) {
            return new Date(logRecord.getMillis()) + " : " + logRecord.getMessage() + "\n";
        }

    }

}

