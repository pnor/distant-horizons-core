package com.seibel.lod.core.logging;

import com.seibel.lod.core.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used to create loggers with specific names.
 *
 * @author James Seibel
 * @version 2022-4-24
 */
public class DhLoggerBuilder
{
	/**
	 * Creates a logger in the format <br>
	 * "ModInfo.Name-className" <br>
	 * For example: <br>
	 * "DistantHorizons-ReflectionHandler" <br><br>
	 *
	 * The suggested way to use this method is like this: <br><br>
	 * <code>
	 * private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	 * </code> <br><br>
	 * By using MethodHandles you don't have to manually enter the class name,
	 * Java figures that out for you. Even in a static context.
	 *
	 * @param className name of the class this logger will be named after.
	 */
	public static Logger getLogger(String className)
	{
		return LogManager.getLogger(ModInfo.NAME + "-" + className);
	}
	public static Logger getLogger(Class<?> clazz)
	{
		return LogManager.getLogger(ModInfo.NAME + "-" + clazz.getSimpleName());
	}
	public static Logger getLogger()
	{
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		String callerClassName = "??";
		for (int i=1; i<stElements.length; i++) {
			StackTraceElement ste = stElements[i];
			if (!ste.getClassName().equals(DhLoggerBuilder.class.getName())
					&& ste.getClassName().indexOf("java.lang.Thread")!=0) {
				callerClassName = ste.getClassName();
				break;
			}
		}
		return LogManager.getLogger(ModInfo.NAME + "-" + callerClassName);
	}
}
