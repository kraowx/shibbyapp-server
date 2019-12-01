package io.github.kraowx.shibbyappserver.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FormattedOutput
{
	public static String get(String str)
	{
		return getTimeString() + " " + str;
	}
	
	private static String getTimeString()
	{
		Date time = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss]");
		return sdf.format(time);
	}
}
