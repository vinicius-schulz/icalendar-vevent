package br.com.honora.icalendar_vevent.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleOccurrenceResponse {
    private UUID scheduleId;
    private String type; // "SCHEDULE", "RDATE", "OVERRIDE"
    private OffsetDateTime start;
    private Integer durationSeconds;
    private OffsetDateTime end;
    private String summary;
    private String notes;

    public ScheduleOccurrenceResponse() {
    }

    public ScheduleOccurrenceResponse(UUID scheduleId, String type, OffsetDateTime start, Integer durationSeconds,
            String summary, String notes) {
        this.scheduleId = scheduleId;
        this.type = type;
        this.start = start;
        this.durationSeconds = durationSeconds;
        this.end = start != null && durationSeconds != null ? start.plusSeconds(durationSeconds) : null;
        this.durationSeconds = durationSeconds;
        this.summary = summary;
        this.notes = notes;
    }

}