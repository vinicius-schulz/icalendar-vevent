package br.com.honora.icalendar_vevent.repository;

import br.com.honora.icalendar_vevent.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    @Query("""
            SELECT DISTINCT s FROM Schedule s
            LEFT JOIN FETCH s.exdates
            LEFT JOIN FETCH s.rdates
            LEFT JOIN FETCH s.overrides
            WHERE s.id = :id
            """)
    Optional<Schedule> findByIdWithChildren(@Param("id") UUID id);

    /**
     * Candidates whose series window overlaps the [from, to] window.
     * Uses indexes on sch_series_start_utc and sch_series_until_utc.
     */
    @Query(value = """
            SELECT *
            FROM tb_sch_schedule s
            WHERE s.sch_series_start_utc <= :to
            	AND (s.sch_series_until_utc IS NULL OR s.sch_series_until_utc >= :from)
            """, nativeQuery = true)
    List<Schedule> findCandidatesBySeriesWindow(@Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /**
     * Candidates that have at least one RDATE that converts (using sch_tzid)
     * to an instant within [from, to].
     */
    @Query(value = """
            SELECT DISTINCT s.*
            FROM tb_sch_schedule s
            JOIN tb_srd_schedule_rdate r ON r.sch_id = s.sch_id
            WHERE (r.srd_rdate_local AT TIME ZONE s.sch_tzid) >= :from
            	AND (r.srd_rdate_local AT TIME ZONE s.sch_tzid) <= :to
            """, nativeQuery = true)
    List<Schedule> findWithRdatesInRange(@Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /**
     * Candidates that have at least one OVERRIDE new start within [from, to]
     * (converted using sch_tzid).
     */
    @Query(value = """
            SELECT DISTINCT s.*
            FROM tb_sch_schedule s
            JOIN tb_sov_schedule_override o ON o.sch_id = s.sch_id
            WHERE (o.sov_new_start_local AT TIME ZONE s.sch_tzid) >= :from
            	AND (o.sov_new_start_local AT TIME ZONE s.sch_tzid) <= :to
            """, nativeQuery = true)
    List<Schedule> findWithOverridesInRange(@Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}