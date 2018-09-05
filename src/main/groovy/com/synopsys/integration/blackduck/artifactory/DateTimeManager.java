package com.synopsys.integration.blackduck.artifactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeManager {
    private final String dateTimePattern;

    public DateTimeManager(final String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public long getTimeFromString(final String dateTimeString) {
        return getDateFromString(dateTimeString).getTime();
    }

    public String getStringFromDate(final Date date) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return date.toInstant().atZone(ZoneOffset.UTC).format(dateTimeFormatter);
    }

    public Date getDateFromString(final String dateTimeString) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneOffset.UTC);
        return Date.from(ZonedDateTime.from(dateTimeFormatter.parse(dateTimeString)).toInstant());
    }

}
