package br.com.honora.icalendar_vevent.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ScheduleOverrideRequest {

    @JsonProperty("recurrenceIdLocal")
    private LocalDateTime recurrenceIdLocal;

    @JsonProperty("newStartLocal")
    private LocalDateTime newStartLocal;

    @JsonProperty("newDurationSeconds")
    private Integer newDurationSeconds; // se nulo, usar durationSeconds do schedule

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("notes")
    private String notes;
}
