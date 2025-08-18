package br.com.honora.icalendar_vevent.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.honora.icalendar_vevent.domain.Schedule;
import br.com.honora.icalendar_vevent.domain.ScheduleExdate;
import br.com.honora.icalendar_vevent.domain.ScheduleOverride;
import br.com.honora.icalendar_vevent.domain.ScheduleRdate;
import br.com.honora.icalendar_vevent.dto.request.ScheduleRequest;
import br.com.honora.icalendar_vevent.dto.request.ScheduleOverrideRequest;
import br.com.honora.icalendar_vevent.dto.request.ForceEndRequest;
import br.com.honora.icalendar_vevent.dto.response.ScheduleOccurrenceResponse;
import br.com.honora.icalendar_vevent.dto.response.ScheduleResponse;
import br.com.honora.icalendar_vevent.repository.ScheduleRepository;
import br.com.honora.icalendar_vevent.utils.DateUtils;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // ========= Mutations on existing schedule (without changing RRULE) =========
    @Transactional
    public void putExdate(UUID scheduleId, String exdateLocalStr) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(exdateLocalStr, "exdateLocal is required");
        LocalDateTime ldt = LocalDateTime.parse(exdateLocalStr);
        boolean exists = s.getExdates().stream().anyMatch(e -> e.getExdateLocal().equals(ldt));
        if (!exists) {
            s.addExdate(ScheduleExdate.builder().exdateLocal(ldt).build());
        }
        s.setHasExdates(true);
        scheduleRepository.save(s);
    }

    @Transactional
    public void deleteExdate(UUID scheduleId, String exdateLocalStr) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(exdateLocalStr, "exdateLocal is required");
        LocalDateTime ldt = LocalDateTime.parse(exdateLocalStr);
        s.getExdates().removeIf(e -> e.getExdateLocal().equals(ldt));
        if (s.getExdates().isEmpty()) s.setHasExdates(false);
        scheduleRepository.save(s);
    }

    @Transactional
    public void putRdate(UUID scheduleId, String rdateLocalStr, Integer durationSeconds) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(rdateLocalStr, "rdateLocal is required");
        LocalDateTime ldt = LocalDateTime.parse(rdateLocalStr);
        Integer dur = Optional.ofNullable(durationSeconds).orElse(s.getDurationSeconds());
        Optional<ScheduleRdate> existing = s.getRdates().stream().filter(r -> r.getRdateLocal().equals(ldt)).findFirst();
        if (existing.isPresent()) {
            existing.get().setDurationSeconds(dur);
        } else {
            s.addRdate(ScheduleRdate.builder().rdateLocal(ldt).durationSeconds(dur).build());
        }
        s.setHasRdates(true);
        scheduleRepository.save(s);
    }

    @Transactional
    public void deleteRdate(UUID scheduleId, String rdateLocalStr) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(rdateLocalStr, "rdateLocal is required");
        LocalDateTime ldt = LocalDateTime.parse(rdateLocalStr);
        s.getRdates().removeIf(r -> r.getRdateLocal().equals(ldt));
        if (s.getRdates().isEmpty()) s.setHasRdates(false);
        scheduleRepository.save(s);
    }

    @Transactional
    public void putOverride(UUID scheduleId, String recurrenceIdLocalStr, ScheduleOverrideRequest req) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(recurrenceIdLocalStr, "recurrenceIdLocal is required");
        Objects.requireNonNull(req.getNewStartLocal(), "newStartLocal is required");
        LocalDateTime rid = LocalDateTime.parse(recurrenceIdLocalStr);
        Integer dur = Optional.ofNullable(req.getNewDurationSeconds()).orElse(s.getDurationSeconds());
        Optional<ScheduleOverride> existing = s.getOverrides().stream()
                .filter(o -> o.getRecurrenceIdLocal().equals(rid)).findFirst();
        if (existing.isPresent()) {
            ScheduleOverride o = existing.get();
            o.setNewStartLocal(req.getNewStartLocal());
            o.setNewDurationSeconds(dur);
            o.setSummary(req.getSummary());
            o.setNotes(req.getNotes());
        } else {
            s.addOverride(ScheduleOverride.builder()
                    .recurrenceIdLocal(rid)
                    .newStartLocal(req.getNewStartLocal())
                    .newDurationSeconds(dur)
                    .summary(req.getSummary())
                    .notes(req.getNotes())
                    .build());
        }
        s.setHasOverrides(true);
        scheduleRepository.save(s);
    }

    @Transactional
    public void deleteOverride(UUID scheduleId, String recurrenceIdLocalStr) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(recurrenceIdLocalStr, "recurrenceIdLocal is required");
        LocalDateTime rid = LocalDateTime.parse(recurrenceIdLocalStr);
        s.getOverrides().removeIf(o -> o.getRecurrenceIdLocal().equals(rid));
        if (s.getOverrides().isEmpty()) s.setHasOverrides(false);
        scheduleRepository.save(s);
    }

    @Transactional
    public void forceEnd(UUID scheduleId, ForceEndRequest req) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        Objects.requireNonNull(req.getSeriesUntilUtc(), "seriesUntilUtc is required");
        // Validation: cannot set until before series start utc
        if (req.getSeriesUntilUtc().isBefore(s.getSeriesStartUtc())) {
            throw new IllegalArgumentException("seriesUntilUtc cannot be before seriesStartUtc");
        }
        s.setSeriesUntilUtc(req.getSeriesUntilUtc());
        scheduleRepository.save(s);
    }

    @Transactional
    public Schedule create(ScheduleRequest req) {
        Schedule s = Schedule.builder()
                .rruleJson(req.getRrule())
                .tzid(req.getTzid())
                .seriesStartLocal(req.getSeriesStartLocal())
                .seriesStartUtc(req.getSeriesStartUtc())
                .seriesUntilUtc(req.getSeriesUntilUtc())
                .durationSeconds(req.getDurationSeconds())
                .summary(req.getSummary())
                .notes(req.getNotes())
                .hasExdates(req.getExdates() != null && !req.getExdates().isEmpty())
                .hasRdates(req.getRdates() != null && !req.getRdates().isEmpty())
                .hasOverrides(req.getOverrides() != null && !req.getOverrides().isEmpty())
                .build();

        // exdates
        Optional.ofNullable(req.getExdates()).ifPresent(list -> list.forEach(ldt -> {
            ScheduleExdate e = ScheduleExdate.builder()
                    .exdateLocal(ldt.getExdateLocal())
                    .build();
            s.addExdate(e);
        }));

        // rdates: atribui duration igual à duração da série se não informado
        // separadamente
        Optional.ofNullable(req.getRdates()).ifPresent(list -> list.forEach(ldt -> {
            ScheduleRdate r = ScheduleRdate.builder()
                    .rdateLocal(ldt.getRdateLocal())
                    .durationSeconds(Optional.ofNullable(ldt.getDurationSeconds()).orElse(s.getDurationSeconds()))
                    .build();
            s.addRdate(r);
        }));

        // overrides
        Optional.ofNullable(req.getOverrides()).ifPresent(list -> list.forEach(ov -> {
            ScheduleOverride o = ScheduleOverride.builder()
                    .recurrenceIdLocal(ov.getRecurrenceIdLocal())
                    .newStartLocal(ov.getNewStartLocal())
                    .newDurationSeconds(Optional.ofNullable(ov.getNewDurationSeconds()).orElse(s.getDurationSeconds()))
                    .summary(ov.getSummary())
                    .notes(ov.getNotes())
                    .build();
            s.addOverride(o);
        }));

        return scheduleRepository.save(s);
    }

    // Lista todos os schedules
    public List<ScheduleResponse> findAll() {
        return scheduleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ScheduleResponse toResponse(Schedule s) {
    return ScheduleResponse.builder()
        .id(s.getId())
        .rruleJson(s.getRruleJson())
        .tzid(s.getTzid())
        .seriesStartLocal(s.getSeriesStartLocal())
        .seriesStartUtc(s.getSeriesStartUtc())
        .seriesUntilUtc(s.getSeriesUntilUtc())
        .durationSeconds(s.getDurationSeconds())
        .summary(s.getSummary())
        .notes(s.getNotes())
        .hasExdates(s.isHasExdates())
        .hasRdates(s.isHasRdates())
        .hasOverrides(s.isHasOverrides())
        .createdAt(s.getCreatedAt())
        .updatedAt(s.getUpdatedAt())
        .exdates(Optional.ofNullable(s.getExdates()).orElseGet(java.util.Set::of).stream()
            .map(e -> br.com.honora.icalendar_vevent.dto.response.ScheduleExdateResponse.builder()
                .id(e.getId())
                .exdateLocal(e.getExdateLocal())
                .build())
            .collect(Collectors.toList()))
        .rdates(Optional.ofNullable(s.getRdates()).orElseGet(java.util.Set::of).stream()
            .map(r -> br.com.honora.icalendar_vevent.dto.response.ScheduleRdateResponse.builder()
                .id(r.getId())
                .rdateLocal(r.getRdateLocal())
                .durationSeconds(r.getDurationSeconds())
                .build())
            .collect(Collectors.toList()))
        .overrides(Optional.ofNullable(s.getOverrides()).orElseGet(java.util.Set::of).stream()
            .map(o -> br.com.honora.icalendar_vevent.dto.response.ScheduleOverrideResponse.builder()
                .id(o.getId())
                .recurrenceIdLocal(o.getRecurrenceIdLocal())
                .newStartLocal(o.getNewStartLocal())
                .newDurationSeconds(o.getNewDurationSeconds())
                .summary(o.getSummary())
                .notes(o.getNotes())
                .build())
            .collect(Collectors.toList()))
        .build();
    }

    // Exclui todos os schedules (batch)
    @Transactional
    public void deleteAll() {
        // deleteAllInBatch evita carregar entidades na memória
        scheduleRepository.deleteAllInBatch();
    }

    /**
     * Retorna ocorrências entre from..to (ambos em UTC).
     * - Usa filtros no banco para reduzir candidatos (janela da série, RDATEs e
     * OVERRIDES no range)
     * - Interpreta RRULE em JSON para gerar ocorrências
     * - Remove EXDATEs para ocorrências RRULE/RDATE do mesmo schedule
     * - Inclui RDATEs e OVERRIDES
     */
    @Transactional(readOnly = true)
    public List<ScheduleOccurrenceResponse> findOccurrencesBetween(String fromStr, String toStr) {
        OffsetDateTime from = DateUtils.parseFlexibleOffsetDateTime(fromStr);
        OffsetDateTime to = DateUtils.parseFlexibleOffsetDateTime(toStr);
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");

        // Phase 1: pre-filter candidate schedules by overlapping series window, rdates,
        // or overrides
        List<Schedule> byWindow = scheduleRepository.findCandidatesBySeriesWindow(from, to);
        List<Schedule> byRdates = scheduleRepository.findWithRdatesInRange(from, to);
        List<Schedule> byOverrides = scheduleRepository.findWithOverridesInRange(from, to);

        // Merge unique candidates
        Map<java.util.UUID, Schedule> candidates = new java.util.LinkedHashMap<>();
        for (Schedule s : byWindow)
            candidates.put(s.getId(), s);
        for (Schedule s : byRdates)
            candidates.putIfAbsent(s.getId(), s);
        for (Schedule s : byOverrides)
            candidates.putIfAbsent(s.getId(), s);

        List<ScheduleOccurrenceResponse> result = new ArrayList<>();

        for (Schedule s : candidates.values()) {
            ZoneId zoneId = ZoneId.of(s.getTzid());

            // Collect EXDATEs (local) and Overrides keyed by recurrence id (local)
            Set<LocalDateTime> exdatesLocal = s.getExdates().stream()
                    .map(ScheduleExdate::getExdateLocal)
                    .collect(Collectors.toSet());

            Map<LocalDateTime, ScheduleOverride> overridesByRecurrence = s.getOverrides().stream()
                    .collect(Collectors.toMap(ScheduleOverride::getRecurrenceIdLocal, o -> o, (a, b) -> a));

            // Use a map to dedupe by UTC start per schedule
            Map<OffsetDateTime, ScheduleOccurrenceResponse> occByStartUtc = new java.util.LinkedHashMap<>();

            // 1) Generate RRULE occurrences within [from, to]
            JsonNode rr = s.getRruleJson();
            if (rr != null && rr.has("freq")) {
                try {
                    String rruleString = buildRruleFromJson(rr);
                    Recur recur = new Recur(rruleString);

                    DateTime seed = new DateTime(Date.from(s.getSeriesStartUtc().toInstant()));
                    DateTime periodStart = new DateTime(Date.from(from.toInstant()));
                    DateTime periodEnd = new DateTime(Date.from(to.toInstant()));
                    Period period = new Period(periodStart, periodEnd);

                    DateList dates = recur.getDates(seed, period.getStart(), period.getEnd(), Value.DATE_TIME);

                    for (Object obj : dates) {
                        DateTime dt = (DateTime) obj;
                        OffsetDateTime occUtc = OffsetDateTime.ofInstant(dt.toInstant(), ZoneOffset.UTC);
                        LocalDateTime occLocal = LocalDateTime.ofInstant(dt.toInstant(), zoneId);

                        // Skip if overridden
                        if (overridesByRecurrence.containsKey(occLocal)) {
                            continue;
                        }

                        // Skip if EXDATE matches
                        if (exdatesLocal.contains(occLocal)) {
                            continue;
                        }

                        // Within [from, to] already ensured by generator; add occurrence
                        ScheduleOccurrenceResponse resp = new ScheduleOccurrenceResponse(
                                s.getId(),
                                "SCHEDULE",
                                occUtc,
                                s.getDurationSeconds(),
                                s.getSummary(),
                                s.getNotes());
                        occByStartUtc.putIfAbsent(resp.getStart(), resp);
                    }
                } catch (Exception ignore) {
                    // If RRULE parsing fails, ignore RRULE occurrences for this schedule
                }
            }

            // 2) Include RDATEs within [from, to] (convert from local using tzid)
            for (ScheduleRdate r : s.getRdates()) {
                LocalDateTime rLocal = r.getRdateLocal();
                OffsetDateTime rUtc = rLocal.atZone(zoneId).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
                if (rUtc.isBefore(from) || rUtc.isAfter(to)) {
                    continue;
                }
                // Skip if EXDATE matches this local start
                if (exdatesLocal.contains(rLocal)) {
                    continue;
                }
                ScheduleOccurrenceResponse resp = new ScheduleOccurrenceResponse(
                        s.getId(),
                        "RDATE",
                        rUtc,
                        Optional.ofNullable(r.getDurationSeconds()).orElse(s.getDurationSeconds()),
                        s.getSummary(),
                        s.getNotes());
                occByStartUtc.putIfAbsent(resp.getStart(), resp);
            }

            // 3) Include Overrides (new start) within [from, to]
            for (ScheduleOverride o : s.getOverrides()) {
                LocalDateTime newLocal = o.getNewStartLocal();
                OffsetDateTime newUtc = newLocal.atZone(zoneId).toOffsetDateTime()
                        .withOffsetSameInstant(ZoneOffset.UTC);
                if (newUtc.isBefore(from) || newUtc.isAfter(to)) {
                    continue;
                }
                // Overrides replace the base recurrence; EXDATE does not remove overrides.
                ScheduleOccurrenceResponse resp = new ScheduleOccurrenceResponse(
                        s.getId(),
                        "OVERRIDE",
                        newUtc,
                        Optional.ofNullable(o.getNewDurationSeconds()).orElse(s.getDurationSeconds()),
                        Optional.ofNullable(o.getSummary()).orElse(s.getSummary()),
                        Optional.ofNullable(o.getNotes()).orElse(s.getNotes()));
                // If an RRULE/RDATE occurrence is at the exact same UTC instant, override wins
                occByStartUtc.put(resp.getStart(), resp);
            }

            // Collect this schedule's occurrences
            result.addAll(occByStartUtc.values());
        }

        // Sort by start asc, then scheduleId
        result.sort(Comparator
                .comparing(ScheduleOccurrenceResponse::getStart)
                .thenComparing(ScheduleOccurrenceResponse::getScheduleId));

        return result;
    }

    // Helper: constrói string RRULE básica a partir do JSON armazenado
    private String buildRruleFromJson(JsonNode rr) {
        StringBuilder sb = new StringBuilder();
        if (rr.has("freq")) {
            sb.append("FREQ=").append(rr.get("freq").asText().toUpperCase());
        }
        if (rr.has("interval") && !rr.get("interval").isNull()) {
            sb.append(";INTERVAL=").append(rr.get("interval").asInt());
        }
        if (rr.has("byday") && rr.get("byday").isArray()) {
            List<String> days = new ArrayList<>();
            rr.get("byday").forEach(node -> {
                if (!node.isNull())
                    days.add(node.asText());
            });
            if (!days.isEmpty())
                sb.append(";BYDAY=").append(String.join(",", days));
        }
        if (rr.has("count") && !rr.get("count").isNull()) {
            sb.append(";COUNT=").append(rr.get("count").asInt());
        } else if (rr.has("until_utc") && !rr.get("until_utc").isNull()) {
            String until = rr.get("until_utc").asText();
            Instant inst = Instant.parse(until);
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
            sb.append(";UNTIL=").append(f.format(inst));
        }
        return sb.toString();
    }

    // ========================= ICS (iCalendar) =========================
    private static final DateTimeFormatter ICS_ZULU = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ICS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    @Transactional(readOnly = true)
    public String buildIcsForSchedule(UUID scheduleId) {
        Schedule s = scheduleRepository.findByIdWithChildren(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        String uid = s.getId().toString() + "@icalendar-vevent"; // personalize se quiser
        String dtStamp = ICS_ZULU.format(Instant.now());

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("PRODID:-//UVIX//Agenda de Plantões//PT-BR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n\r\n");

        // Base VEVENT (série)
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(dtStamp).append("\r\n");
        if (s.getSummary() != null)
            sb.append("SUMMARY:").append(escapeText(s.getSummary())).append("\r\n");
        if (s.getNotes() != null)
            sb.append("DESCRIPTION:").append(escapeText(s.getNotes())).append("\r\n");

        // DTSTART local com TZID
        String tzid = s.getTzid();
        String dtStartLocal = ICS_LOCAL.format(s.getSeriesStartLocal());
        sb.append("DTSTART;TZID=").append(tzid).append(":").append(dtStartLocal).append("\r\n");

        // DURATION ISO-8601
        sb.append("DURATION:").append(toISODuration(s.getDurationSeconds())).append("\r\n");

        // RRULE a partir do JSON
        JsonNode rr = s.getRruleJson();
        if (rr != null && rr.has("freq")) {
            String rruleString = buildRruleFromJson(rr);
            if (!rruleString.isBlank()) {
                sb.append("RRULE:").append(rruleString).append("\r\n");
            }
        }

        // EXDATE (em linhas, podendo agrupar por TZID igual ao DTSTART)
        if (s.isHasExdates() && s.getExdates() != null && !s.getExdates().isEmpty()) {
            String exdates = s.getExdates().stream()
                    .map(ScheduleExdate::getExdateLocal)
                    .sorted()
                    .map(ldt -> ICS_LOCAL.format(ldt))
                    .collect(Collectors.joining(","));
            if (!exdates.isBlank()) {
                sb.append("EXDATE;TZID=").append(tzid).append(":").append(exdates).append("\r\n");
            }
        }

        // RDATEs agregados no evento mestre
        if (s.isHasRdates() && s.getRdates() != null && !s.getRdates().isEmpty()) {
            String rdates = s.getRdates().stream()
                    .map(ScheduleRdate::getRdateLocal)
                    .sorted()
                    .map(ldt -> ICS_LOCAL.format(ldt))
                    .collect(Collectors.joining(","));
            if (!rdates.isBlank()) {
                sb.append("RDATE;TZID=").append(tzid).append(":").append(rdates).append("\r\n");
            }
        }

        sb.append("END:VEVENT\r\n\r\n");

        // Overrides – um VEVENT por override com RECURRENCE-ID
        if (s.isHasOverrides() && s.getOverrides() != null) {
            for (ScheduleOverride o : s.getOverrides()) {
                sb.append("BEGIN:VEVENT\r\n");
                sb.append("UID:").append(uid).append("\r\n");
                sb.append("DTSTAMP:").append(dtStamp).append("\r\n");
                sb.append("RECURRENCE-ID;TZID=").append(tzid).append(":")
                        .append(ICS_LOCAL.format(o.getRecurrenceIdLocal())).append("\r\n");
                if (o.getSummary() != null)
                    sb.append("SUMMARY:").append(escapeText(o.getSummary())).append("\r\n");
                if (o.getNotes() != null)
                    sb.append("DESCRIPTION:").append(escapeText(o.getNotes())).append("\r\n");
                sb.append("DTSTART;TZID=").append(tzid).append(":").append(ICS_LOCAL.format(o.getNewStartLocal()))
                        .append("\r\n");
                sb.append("DURATION:").append(toISODuration(
                        Optional.ofNullable(o.getNewDurationSeconds()).orElse(s.getDurationSeconds())))
                        .append("\r\n");
                sb.append("END:VEVENT\r\n\r\n");
            }
        }

    // (Sem VEVENTs separados para RDATE; usamos RDATE no mestre.)

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String toISODuration(Integer seconds) {
        if (seconds == null || seconds <= 0)
            return "PT0S";
        long s = seconds;
        long h = s / 3600;
        s %= 3600;
        long m = s / 60;
        s %= 60;
        StringBuilder b = new StringBuilder("PT");
        if (h > 0)
            b.append(h).append('H');
        if (m > 0)
            b.append(m).append('M');
        if (s > 0 || (h == 0 && m == 0))
            b.append(s).append('S');
        return b.toString();
    }

    // Escapa texto conforme RFC5545 (vírgula, ponto e vírgula, barra invertida, quebras)
    private static String escapeText(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}