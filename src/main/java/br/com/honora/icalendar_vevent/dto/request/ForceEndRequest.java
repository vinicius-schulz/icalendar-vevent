package br.com.honora.icalendar_vevent.dto.request;

import java.time.OffsetDateTime;

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
public class ForceEndRequest {
    @JsonProperty("seriesUntilUtc")
    private OffsetDateTime seriesUntilUtc;
}
