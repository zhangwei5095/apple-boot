package com.appleframework.boot.core.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Log4jUtils {

	public static Level getRootLoggerLevel() {
		Logger logger = LogManager.getLoggerRepository().getRootLogger();
		return logger.getLevel();
	}

}