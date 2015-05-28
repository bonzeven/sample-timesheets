/*
 * Copyright (c) ${YEAR} ${PACKAGE_NAME}
 */

package com.haulmont.timesheets.global;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.time.DateUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author gorelov
 * @version $Id$
 */

public final class DateTimeUtils {
    public static final String TIME_FORMAT = "HH:mm";

    private DateTimeUtils() {
    }

    public static Calendar getCalendarWithoutTime(Date date) {
        return getCalendarWithoutTime(date, userSession().getLocale());
    }

    public static Calendar getCalendarWithoutTime(Date date, Locale locale) {
        Calendar calendar = Calendar.getInstance(locale);
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Date getDateWithoutTime(Date date) {
        return getCalendarWithoutTime(date).getTime();
    }

    public static Date getFirstDayOfWeek(Date date) {
        return getFirstDayOfWeek(date, userSession().getLocale());
    }

    public static Date getFirstDayOfWeek(Date date, Locale locale) {
        Calendar calendar = getCalendarWithoutTime(date, locale);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        return calendar.getTime();
    }

    public static Date getLastDayOfWeek(Date date) {
        return DateUtils.addDays(getFirstDayOfWeek(date), 6);
    }

    public static Date getSpecificDayOfWeek(Date date, int javaCalendarDayNumber) {
        return getSpecificDayOfWeek(date, javaCalendarDayNumber, userSession().getLocale());
    }

    public static Date getSpecificDayOfWeek(Date date, int javaCalendarDayNumber, Locale locale) {
        Calendar calendar = getCalendarWithoutTime(date, locale);
        calendar.set(Calendar.DAY_OF_WEEK, javaCalendarDayNumber);
        return calendar.getTime();
    }

    public static Date getFirstDayOfMonth(Date date) {
        Calendar calendar = getCalendarWithoutTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    public static Date getLastDayOfMonth(Date date) {
        Calendar calendar = getCalendarWithoutTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    public static int getCalendarDayOfWeek(Date date) {
        return DateUtils.toCalendar(date).get(Calendar.DAY_OF_WEEK);
    }

    public static BigDecimal timeStringToBigDecimal(String time) {
        if (time.contains(":")) {
            String[] parts = time.split(":");
            return BigDecimal.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0);
        } else {
            TimeParser timeParser = AppBeans.get(TimeParser.NAME);
            return BigDecimal.valueOf(timeParser.findHours(time) + timeParser.findMinutes(time) / 60.0);
        }
    }

    public static BigDecimal dateToBigDecimal(@Nullable Date date) {
        if (date == null) {
            return BigDecimal.ZERO;
        }
        HoursAndMinutes time = new HoursAndMinutes();
        Calendar calendar = DateUtils.toCalendar(date);
        time.addHours(calendar.get(Calendar.HOUR_OF_DAY));
        time.addMinutes(calendar.get(Calendar.MINUTE));
        return time.getTime();
    }

    public static DateFormat getDateFormat() {
        return new SimpleDateFormat(messages().getMainMessage("dateFormat"));
    }

    private static Messages messages() {
        return AppBeans.get(Messages.NAME, Messages.class);
    }

    private static UserSession userSession() {
        return AppBeans.get(UserSessionSource.class).getUserSession();
    }
}
