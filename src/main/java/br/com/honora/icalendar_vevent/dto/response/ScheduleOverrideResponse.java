package br.com.honora.icalendar_vevent.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleOverrideResponse {
    private UUID id;
    private LocalDateTime recurrenceIdLocal;
    private LocalDateTime newStartLocal;
    private Integer newDurationSeconds;
    private String summary;
    private String notes;
}
