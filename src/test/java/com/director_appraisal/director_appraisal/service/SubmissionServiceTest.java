package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TableDataPromotionService tableDataPromotionService;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void approvalStoresReportMetadataAndPromotesNormalizedTables() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("director@dypiu.ac.in")
                .auditType("academic")
                .status("AUDITOR_COMPLETED")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .version(1)
                .build();
        User iqac = User.builder()
                .id(9L)
                .email("iqac@dypiu.ac.in")
                .name("IQAC User")
                .role("iqac")
                .designation("IQAC")
                .build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission approved = submissionService.reviewSubmission(
                123L,
                "APPROVED",
                "Final remarks",
                "INTERNAL",
                "2025-26",
                1,
                "{\"schoolName\":\"School\"}",
                "{}",
                "[]",
                iqac
        );

        assertEquals("APPROVED", approved.getStatus());
        assertEquals("INTERNAL", approved.getReportCategory());
        assertEquals("2025-26", approved.getAuditCycle());
        assertEquals(123L, approved.getRootSubmissionId());
        assertEquals(9L, approved.getApprovedByUserId());
        assertEquals("IQAC User", approved.getApprovedByName());
        assertNotNull(approved.getApprovedAt());
        verify(tableDataPromotionService).syncNormalizedTablesAndClearSnapshots(approved);
    }

    @Test
    void approvedSubmissionCannotBeEdited() {
        Submission approved = Submission.builder()
                .id(123L)
                .email("director@dypiu.ac.in")
                .auditType("academic")
                .status("APPROVED")
                .version(1)
                .build();
        User owner = User.builder()
                .email("director@dypiu.ac.in")
                .role("director")
                .build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(approved));

        assertThrows(SecurityException.class, () -> submissionService.updateSubmission(
                123L,
                owner,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"changed\":true}",
                "{}",
                "[]"
        ));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void nextCycleClonesApprovedSourceAndClearsCurrentAuditorSections() {
        Submission source = Submission.builder()
                .id(123L)
                .email("director@dypiu.ac.in")
                .auditType("academic")
                .school("School of Computer Science & Applications")
                .submittedBy("Director")
                .status("APPROVED")
                .version(1)
                .rootSubmissionId(123L)
                .valuesData("{\"schoolName\":\"School\",\"partEObservation\":\"old\",\"__auditSignOff\":{\"auditedBy\":{\"name\":\"Auditor\"}},\"other\":{\"keep\":\"yes\",\"partERemark\":\"old\"}}")
                .tablesData("{\"studentStrength\":[{\"srNo\":\"1\"}],\"partEObservations\":[{\"details\":\"old\"}]}")
                .attachments("[{\"name\":\"proof.pdf\"}]")
                .build();
        User vc = User.builder()
                .id(7L)
                .name("VC User")
                .role("vice-chancellor")
                .build();

        when(submissionRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(source));
        when(submissionRepository.findMaxVersionInLineage(123L)).thenReturn(1);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            saved.setId(124L);
            return saved;
        });

        Submission next = submissionService.createNextCycle(123L, vc, true, 123L, 2, "EXTERNAL");

        assertEquals("APPROVED", source.getStatus());
        assertEquals("DRAFT", next.getStatus());
        assertEquals(2, next.getVersion());
        assertEquals(123L, next.getRootSubmissionId());
        assertEquals(123L, next.getParentSubmissionId());
        assertEquals(123L, next.getPreviousApprovedSubmissionId());
        assertEquals(1, next.getCreatedFromVersion());
        assertEquals("external", next.getForwardedAuditorType());
        assertTrue(next.getValuesData().contains("schoolName"));
        assertTrue(next.getValuesData().contains("keep"));
        assertFalse(next.getValuesData().toLowerCase().contains("parte"));
        assertFalse(next.getValuesData().contains("__auditSignOff"));
        assertFalse(next.getTablesData().toLowerCase().contains("parte"));
        assertTrue(next.getTablesData().contains("studentStrength"));

        ArgumentCaptor<Snapshot> snapshotCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        assertEquals(124L, snapshotCaptor.getValue().getSubmissionId());
        assertEquals("DRAFT", snapshotCaptor.getValue().getStatus());
    }

    @Test
    void versionHistoryReturnsApprovedLineageFromLaterCycle() {
        Submission current = Submission.builder()
                .id(124L)
                .rootSubmissionId(123L)
                .status("DRAFT")
                .version(2)
                .build();
        Submission approved = Submission.builder()
                .id(123L)
                .rootSubmissionId(123L)
                .status("APPROVED")
                .version(1)
                .auditCycle("2025-26")
                .reportCategory("INTERNAL")
                .valuesData("{\"schoolName\":\"School\"}")
                .tablesData("{}")
                .attachments("[]")
                .approvedByName("IQAC User")
                .build();

        when(submissionRepository.findById(124L)).thenReturn(Optional.of(current));
        when(submissionRepository.findLineage(123L)).thenReturn(List.of(approved, current));

        List<Map<String, Object>> history = submissionService.getVersionHistoryForSubmission(124L);

        assertEquals(1, history.size());
        assertEquals(123L, history.get(0).get("id"));
        assertEquals(1, history.get(0).get("version"));
        assertEquals("INTERNAL", history.get(0).get("reportCategory"));
        assertEquals("IQAC User", history.get(0).get("approvedByName"));
    }
}
