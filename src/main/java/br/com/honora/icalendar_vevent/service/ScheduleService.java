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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.honora.icalendar_vevent.domain.Schedule;
import br.com.honora.icalendar_vevent.domain.ScheduleExdate;
import br.com.honora.icalendar_vevent.domain.ScheduleOverride;
import br.com.honora.icalendar_vevent.domain.ScheduleRdate;
import br.com.honora.icalendar_vevent.dto.request.ScheduleRequest;
import br.com.honora.icalendar_vevent.dto.response.ScheduleOccurrenceResponse;
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
                .hasOverrides(false)
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
                    .durationSeconds(s.getDurationSeconds())
                    .build();
            s.addRdate(r);
        }));

        return scheduleRepository.save(s);
    }

    // Lista todos os schedules
    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
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
}