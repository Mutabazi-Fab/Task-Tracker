package com.throughline.taskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** No `teamLeader` field — leadership is scoped per-membership via {@link TeamMember#isLeader},
 *  since a person can lead one team while being a plain member of another. */
@Entity
@Table(name = "teams", indexes = @Index(name = "idx_teams_created_by_id", columnList = "created_by_id"))
@Getter
@Setter
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    /** The Director who created this team. Nullable only for teams that existed before this field was added. */
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private Person createdBy;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
