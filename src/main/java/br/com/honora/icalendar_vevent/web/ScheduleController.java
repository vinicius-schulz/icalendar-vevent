package br.com.honora.icalendar_vevent.web;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.honora.icalendar_vevent.domain.Schedule;
import br.com.honora.icalendar_vevent.dto.request.ScheduleRequest;
import br.com.honora.icalendar_vevent.dto.response.ScheduleOccurrenceResponse;
import br.com.honora.icalendar_vevent.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "Create schedule", description = "Cria um schedule (rrule armazenado como JSON) com exdates/rdates.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScheduleRequest.class), examples = {
            @ExampleObject(value = "{\"rrule\":{\"freq\":\"WEEKLY\",\"interval\":1,\"byday\":[\"TH\"]},\"tzid\":\"America/Sao_Paulo\",\"seriesStartLocal\":\"2025-09-04T15:00:00\",\"seriesStartUtc\":\"2025-09-04T18:00:00Z\",\"seriesUntilUtc\":null,\"durationSeconds\":3600,\"summary\":\"Reunião semanal\",\"notes\":\"Exemplo\",\"exdates\":[{\"exdateLocal\":\"2025-09-11T15:00:00\"}],\"rdates\":[{\"rdateLocal\":\"2025-09-11T15:00:00\"}]}") }))
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduleRequest req) {
        Schedule saved = scheduleService.create(req);
        return ResponseEntity.created(URI.create("/api/schedules/" + saved.getId())).body(saved.getId());
    }

    @Operation(summary = "List schedules", description = "Retorna a lista de schedules persistidos.")
    @ApiResponse(responseCode = "200", description = "Lista de schedules", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Schedule.class), examples = @ExampleObject(value = "[{ \"schId\": \"550e8400-e29b-41d4-a716-446655440000\", \"schRruleJson\": { \"freq\":\"WEEKLY\",\"interval\":1,\"byday\":[\"TH\"],\"exdates_local\":[\"2025-09-07T18:00:00\"],\"rdates_local\":[] }, \"schTzid\": \"America/Sao_Paulo\", \"schSeriesStartLocal\": \"2025-09-04T15:00:00\", \"schSeriesStartUtc\": \"2025-09-04T18:00:00Z\", \"schSeriesUntilUtc\": null, \"schDurationSeconds\": 3600, \"schSummary\": \"Reunião semanal\", \"schNotes\": \"Exemplo\", \"schHasExdates\": true, \"schHasRdates\": false, \"schHasOverrides\": false, \"schCreatedAt\": null, \"schUpdatedAt\": null, \"exdates\": [], \"rdates\": [], \"overrides\": [] }]")))
    @GetMapping
    public ResponseEntity<List<Schedule>> list() {
        List<Schedule> list = scheduleService.findAll();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Delete all schedules", description = "Remove todos os schedules do banco. Use com cuidado; ação irreversível.")
    @ApiResponse(responseCode = "204", description = "Todos os schedules foram removidos", content = @Content)
    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        scheduleService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List occurrences between intervalo", description = "Retorna ocorrências (após aplicar EXDATEs) entre from e to. Params em ISO-8601 (UTC recomendado).")
    @GetMapping("/occurrences")
    public ResponseEntity<List<ScheduleOccurrenceResponse>> occurrences(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {

        OffsetDateTime fromOdt = parseFlexibleOffsetDateTime(from);
        OffsetDateTime toOdt = parseFlexibleOffsetDateTime(to);

        List<ScheduleOccurrenceResponse> list = scheduleService.findOccurrencesBetween(fromOdt, toOdt);
        return ResponseEntity.ok(list);
    }

    /**
     * Parses common ISO-8601 inputs into OffsetDateTime.
     * Accepts:
     * - 2025-09-04T00:00:00Z (UTC)
     * - 2025-09-04T00:00:00-03:00 (explicit offset)
     * - 2025-09-04T00:00:00 (no offset -> assumed system default zone)
     * - 2025-09-04 (date only -> midnight at system default zone)
     */
    private static OffsetDateTime parseFlexibleOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parameter is required and cannot be blank");
        }
        String v = value.trim();
        // 1) Try full OffsetDateTime (with offset or Z)
        try {
            return OffsetDateTime.parse(v, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}
        // 2) Try instant (e.g., 2025-09-04T00:00:00Z)
        try {
            Instant i = Instant.parse(v);
            return i.atOffset(ZoneOffset.UTC);
        } catch (Exception ignored) {}
        // 3) LocalDateTime without offset -> assume system default zone
        try {
            LocalDateTime ldt = LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZoneId zone = ZoneId.systemDefault();
            return ldt.atZone(zone).toOffsetDateTime();
        } catch (Exception ignored) {}
        // 4) LocalDate only -> start of day at system default zone
        try {
            LocalDate ld = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            ZoneId zone = ZoneId.systemDefault();
            return ld.atStartOfDay(zone).toOffsetDateTime();
        } catch (Exception ignored) {}

        throw new IllegalArgumentException(
                "Invalid date-time format: '" + value + "'. Use ISO-8601, e.g. 2025-09-04T00:00:00Z or 2025-09-04T00:00:00-03:00");
    }
}