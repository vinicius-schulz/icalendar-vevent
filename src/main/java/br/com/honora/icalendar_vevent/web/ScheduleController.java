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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.honora.icalendar_vevent.domain.Schedule;
import br.com.honora.icalendar_vevent.dto.request.ScheduleRequest;
import br.com.honora.icalendar_vevent.dto.request.ScheduleOverrideRequest;
import br.com.honora.icalendar_vevent.dto.request.ForceEndRequest;
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
    public ResponseEntity<byte[]> exportIcs(@PathVariable("id") UUID id) {
        String ics = scheduleService.buildIcsForSchedule(id);
        byte[] bytes = ics.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "calendar"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=calendar-" + id + ".ics");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @Operation(summary = "Upsert EXDATE (idempotente)", description = "Cria ou garante a existência de um EXDATE para a data/hora local informada.")
    @PutMapping("/{id}/exdates/{exdateLocal}")
    public ResponseEntity<Void> putExdate(@PathVariable("id") UUID id, @PathVariable("exdateLocal") String exdateLocal) {
        scheduleService.putExdate(id, exdateLocal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove EXDATE")
    @DeleteMapping("/{id}/exdates/{exdateLocal}")
    public ResponseEntity<Void> deleteExdate(@PathVariable("id") UUID id, @PathVariable("exdateLocal") String exdateLocal) {
        scheduleService.deleteExdate(id, exdateLocal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upsert RDATE (idempotente)", description = "Cria ou atualiza um RDATE pela data/hora local da ocorrência; durationSeconds é opcional.")
    @PutMapping("/{id}/rdates/{rdateLocal}")
    public ResponseEntity<Void> putRdate(
            @PathVariable("id") UUID id,
            @PathVariable("rdateLocal") String rdateLocal,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
        scheduleService.putRdate(id, rdateLocal, durationSeconds);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove RDATE")
    @DeleteMapping("/{id}/rdates/{rdateLocal}")
    public ResponseEntity<Void> deleteRdate(@PathVariable("id") UUID id, @PathVariable("rdateLocal") String rdateLocal) {
        scheduleService.deleteRdate(id, rdateLocal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upsert OVERRIDE (idempotente)", description = "Cria ou atualiza um override pela recurrenceIdLocal; newStartLocal é obrigatório.")
    @PutMapping("/{id}/overrides/{recurrenceIdLocal}")
    public ResponseEntity<Void> putOverride(
            @PathVariable("id") UUID id,
            @PathVariable("recurrenceIdLocal") String recurrenceIdLocal,
            @RequestBody ScheduleOverrideRequest req) {
        scheduleService.putOverride(id, recurrenceIdLocal, req);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove OVERRIDE")
    @DeleteMapping("/{id}/overrides/{recurrenceIdLocal}")
    public ResponseEntity<Void> deleteOverride(@PathVariable("id") UUID id, @PathVariable("recurrenceIdLocal") String recurrenceIdLocal) {
        scheduleService.deleteOverride(id, recurrenceIdLocal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Força encerramento da série (PATCH seriesUntilUtc)")
    @PatchMapping("/{id}/series-until")
    public ResponseEntity<Void> forceEnd(@PathVariable("id") UUID id, @RequestBody ForceEndRequest req) {
        scheduleService.forceEnd(id, req);
        return ResponseEntity.noContent().build();
    }

}