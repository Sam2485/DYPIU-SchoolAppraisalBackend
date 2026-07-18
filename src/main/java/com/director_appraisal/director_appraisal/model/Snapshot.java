package com.director_appraisal.director_appraisal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private LocalDateTime savedAt;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String valuesData;

    @Column(columnDefinition = "TEXT")
    private String tablesData;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    private Integer version;
    private String academicYear;
    private String auditCycle;
    private String schoolGroup;

    @Column(columnDefinition = "TEXT")
    private String forwardedAdministrativePosts;

    @Column(columnDefinition = "TEXT")
    private String forwardedToAuditorPosts;

    public String getValuesData() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(valuesData);
    }

    public String getTablesData() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(tablesData);
    }

    public String getAttachments() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(attachments);
    }
}
