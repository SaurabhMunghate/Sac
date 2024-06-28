/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.document;

import com.shatam.shatamindex.search.NumericRangeQuery;
import com.shatam.shatamindex.util.NumericUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTools {

	final static TimeZone GMT = TimeZone.getTimeZone("GMT");

	private static final ThreadLocal<Calendar> TL_CAL = new ThreadLocal<Calendar>() {
		@Override
		protected Calendar initialValue() {
			return Calendar.getInstance(GMT, Locale.US);
		}
	};

	private static final ThreadLocal<SimpleDateFormat[]> TL_FORMATS = new ThreadLocal<SimpleDateFormat[]>() {
		@Override
		protected SimpleDateFormat[] initialValue() {
			SimpleDateFormat[] arr = new SimpleDateFormat[Resolution.MILLISECOND.formatLen + 1];
			for (Resolution resolution : Resolution.values()) {
				arr[resolution.formatLen] = (SimpleDateFormat) resolution.format
						.clone();
			}
			return arr;
		}
	};

	private DateTools() {
	}

	public static String dateToString(Date date, Resolution resolution) {
		return timeToString(date.getTime(), resolution);
	}

	public static String timeToString(long time, Resolution resolution) {
		final Date date = new Date(round(time, resolution));
		return TL_FORMATS.get()[resolution.formatLen].format(date);
	}

	public static long stringToTime(String dateString) throws ParseException {
		return stringToDate(dateString).getTime();
	}

	public static Date stringToDate(String dateString) throws ParseException {
		try {
			return TL_FORMATS.get()[dateString.length()].parse(dateString);
		} catch (Exception e) {
			throw new ParseException("Input is not a valid date string: "
					+ dateString, 0);
		}
	}

	public static Date round(Date date, Resolution resolution) {
		return new Date(round(date.getTime(), resolution));
	}

	@SuppressWarnings("fallthrough")
	public static long round(long time, Resolution resolution) {
		final Calendar calInstance = TL_CAL.get();
		calInstance.setTimeInMillis(time);

		switch (resolution) {

		case YEAR:
			calInstance.set(Calendar.MONTH, 0);
		case MONTH:
			calInstance.set(Calendar.DAY_OF_MONTH, 1);
		case DAY:
			calInstance.set(Calendar.HOUR_OF_DAY, 0);
		case HOUR:
			calInstance.set(Calendar.MINUTE, 0);
		case MINUTE:
			calInstance.set(Calendar.SECOND, 0);
		case SECOND:
			calInstance.set(Calendar.MILLISECOND, 0);
		case MILLISECOND:

			break;
		default:
			throw new IllegalArgumentException("unknown resolution "
					+ resolution);
		}
		return calInstance.getTimeInMillis();
	}

	public static enum Resolution {

		YEAR(4), MONTH(6), DAY(8), HOUR(10), MINUTE(12), SECOND(14), MILLISECOND(
				17);

		final int formatLen;
		final SimpleDateFormat format;

		Resolution(int formatLen) {
			this.formatLen = formatLen;

			this.format = new SimpleDateFormat("yyyyMMddHHmmssSSS".substring(0,
					formatLen), Locale.US);
			this.format.setTimeZone(GMT);
		}

		@Override
		public String toString() {
			return super.toString().toLowerCase(Locale.ENGLISH);
		}

	}

}
