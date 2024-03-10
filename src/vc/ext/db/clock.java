package vc.ext.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class clock {
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
	public static Date dailyDate;
	public static Date campaignDate;
	public static Date getDate() throws ParseException {
		return dateFormat.parse(dateFormat.format(new Date()));
	}
	public static long getHourDifference(Date d1, Date d2) throws ParseException {
		long diffInMil = Math.abs(d2.getTime() - d1.getTime());
		return TimeUnit.HOURS.convert(diffInMil, TimeUnit.MILLISECONDS);
	}
	public static boolean getDayDifference(Date d1, Date d2) throws ParseException {
		Date d1_formatted = dayFormat.parse(dayFormat.format(d1));
		Date d2_formatted = dayFormat.parse(dayFormat.format(d2));
		return d2_formatted.after(d1_formatted);
	}
	public static boolean getMonthDifference(Date d1, Date d2) throws ParseException {
		Date d1_formatted = monthFormat.parse(monthFormat.format(d1));
		Date d2_formatted = monthFormat.parse(monthFormat.format(d2));
		return d2_formatted.after(d1_formatted);
	}
}
