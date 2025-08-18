package br.com.honora.icalendar_vevent.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    /**
     * Parses common ISO-8601 inputs into OffsetDateTime.
     * Accepts:
     * - 2025-09-04T00:00:00Z (UTC)
     * - 2025-09-04T00:00:00-03:00 (explicit offset)
     * - 2025-09-04T00:00:00 (no offset -> assumed system default zone)
     * - 2025-09-04 (date only -> midnight at system default zone)
     */
    public static OffsetDateTime parseFlexibleOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parameter is required and cannot be blank");
        }
        String v = value.trim();
        // 1) Try full OffsetDateTime (with offset or Z)
        try {
            return OffsetDateTime.parse(v, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
        }
        // 2) Try instant (e.g., 2025-09-04T00:00:00Z)
        try {
            Instant i = Instant.parse(v);
            return i.atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        // 3) LocalDateTime without offset -> assume system default zone
        try {
            LocalDateTime ldt = LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZoneId zone = ZoneId.systemDefault();
            return ldt.atZone(zone).toOffsetDateTime();
        } catch (Exception ignored) {
        }
        // 4) LocalDate only -> start of day at system default zone
        try {
            LocalDate ld = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            ZoneId zone = ZoneId.systemDefault();
            return ld.atStartOfDay(zone).toOffsetDateTime();
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException(
                "Invalid date-time format: '" + value
                        + "'. Use ISO-8601, e.g. 2025-09-04T00:00:00Z or 2025-09-04T00:00:00-03:00");
    }

}
