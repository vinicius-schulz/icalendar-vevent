package br.com.honora.icalendar_vevent.dto.request;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    @JsonProperty("rrule")
    private JsonNode rrule; // ou Map<String, Object>

    @JsonProperty("tzid")
    private String tzid;

    @JsonProperty("seriesStartLocal")
    private LocalDateTime seriesStartLocal;

    @JsonProperty("seriesStartUtc")
    private OffsetDateTime seriesStartUtc;

    @JsonProperty("seriesUntilUtc")
    private OffsetDateTime seriesUntilUtc;

    @JsonProperty("durationSeconds")
    private Integer durationSeconds;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("notes")
    private String notes;

    // Arrays “de fora” (se você estiver usando)
    @JsonProperty("exdates")
    private List<ScheduleExdateRequest> exdates;

    @JsonProperty("rdates")
    private List<ScheduleRdateRequest> rdates;
}
