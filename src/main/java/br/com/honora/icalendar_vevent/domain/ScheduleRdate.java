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
@Table(name = "tb_srd_schedule_rdate", uniqueConstraints = {
        @UniqueConstraint(name = "uq_srd_sch_rdate", columnNames = { "sch_id", "srd_rdate_local" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ScheduleRdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "srd_id", nullable = false, updatable = false)
    private UUID id;

    // timestamp (sem timezone)
    @Column(name = "srd_rdate_local", nullable = false)
    private LocalDateTime rdateLocal;

    @Column(name = "srd_duration_seconds", nullable = false)
    private Integer durationSeconds;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_srd_sch"))
    @JsonBackReference
    private Schedule schedule;
}
