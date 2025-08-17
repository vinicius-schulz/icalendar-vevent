package br.com.honora.icalendar_vevent.domain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tb_sch_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "exdates", "rdates", "overrides" })
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sch_id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON) // <- chave para json/jsonb no Hibernate 6
    @Column(name = "sch_rrule_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode rruleJson; // antes era String? Troque para JsonNode (ou Map<String,Object>)

    @Column(name = "sch_tzid", nullable = false)
    private String tzid;

    // timestamp (sem timezone)
    @Column(name = "sch_series_start_local", nullable = false)
    private LocalDateTime seriesStartLocal;

    // timestamptz
    @Column(name = "sch_series_start_utc", nullable = false)
    private OffsetDateTime seriesStartUtc;

    // timestamptz
    @Column(name = "sch_series_until_utc")
    private OffsetDateTime seriesUntilUtc;

    @Column(name = "sch_duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "sch_summary")
    private String summary;

    @Column(name = "sch_notes")
    private String notes;

    @Column(name = "sch_has_exdates", nullable = false)
    private boolean hasExdates;

    @Column(name = "sch_has_rdates", nullable = false)
    private boolean hasRdates;

    @Column(name = "sch_has_overrides", nullable = false)
    private boolean hasOverrides;

    // Gerenciados pelo banco (DEFAULT now()), então insertable/updatable = false
    @Column(name = "sch_created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sch_updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // Relacionamentos (one-to-many)
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ScheduleExdate> exdates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ScheduleRdate> rdates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ScheduleOverride> overrides = new LinkedHashSet<>();

    // helpers opcionais para manter ambos os lados sincronizados
    public void addExdate(ScheduleExdate e) {
        e.setSchedule(this);
        this.exdates.add(e);
    }

    public void addRdate(ScheduleRdate r) {
        r.setSchedule(this);
        this.rdates.add(r);
    }

    public void addOverride(ScheduleOverride o) {
        o.setSchedule(this);
        this.overrides.add(o);
    }
}
