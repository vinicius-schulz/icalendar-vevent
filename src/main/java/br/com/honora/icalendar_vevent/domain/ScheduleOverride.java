package br.com.honora.icalendar_vevent.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_sov_schedule_override", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sov_sch_recurrence", columnNames = { "sch_id", "sov_recurrence_id_local" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sov_id", nullable = false, updatable = false)
    private UUID id;

    // RECURRENCE-ID original (timestamp sem timezone)
    @Column(name = "sov_recurrence_id_local", nullable = false)
    private LocalDateTime recurrenceIdLocal;

    // novo início (timestamp sem timezone)
    @Column(name = "sov_new_start_local", nullable = false)
    private LocalDateTime newStartLocal;

    @Column(name = "sov_new_duration_seconds", nullable = false)
    private Integer newDurationSeconds;

    @Column(name = "sov_summary")
    private String summary;

    @Column(name = "sov_notes")
    private String notes;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sov_sch"))
    @JsonBackReference
    private Schedule schedule;
}
