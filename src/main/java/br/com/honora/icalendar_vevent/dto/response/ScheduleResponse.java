package br.com.honora.icalendar_vevent.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {
    private UUID id;
    private JsonNode rruleJson;
    private String tzid;
    private LocalDateTime seriesStartLocal;
    private OffsetDateTime seriesStartUtc;
    private OffsetDateTime seriesUntilUtc;
    private Integer durationSeconds;
    private String summary;
    private String notes;
    private boolean hasExdates;
    private boolean hasRdates;
    private boolean hasOverrides;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // children
    private List<ScheduleExdateResponse> exdates;
    private List<ScheduleRdateResponse> rdates;
    private List<ScheduleOverrideResponse> overrides;
}
