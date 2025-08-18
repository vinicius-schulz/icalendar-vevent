package br.com.honora.icalendar_vevent.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import br.com.honora.icalendar_vevent.dto.response.ScheduleResponse;
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
    @ApiResponse(responseCode = "200", description = "Lista de schedules", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScheduleResponse.class)))
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> list() {
        List<ScheduleResponse> list = scheduleService.findAll();
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

        List<ScheduleOccurrenceResponse> list = scheduleService.findOccurrencesBetween(from, to);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Exporta um Schedule como .ics", description = "Gera um arquivo iCalendar (text/calendar) com VEVENT mestre (RRULE/EXDATE/RDATE) e VEVENTs de overrides.")
    @GetMapping(value = "/{id}/calendar.ics")
    public ResponseEntity<byte[]> exportIcs(@org.springframework.web.bind.annotation.PathVariable("id") UUID id) {
        String ics = scheduleService.buildIcsForSchedule(id);
        byte[] bytes = ics.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "calendar"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=calendar-" + id + ".ics");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

}