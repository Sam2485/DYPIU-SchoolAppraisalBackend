package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.SubmissionAuditorAssignment;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionAuditorAssignmentRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserAdministrativePostRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

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

    @Mock
    private SubmissionAuditorAssignmentRepository auditorAssignmentRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAdministrativePostRepository userAdministrativePostRepository;

    @Mock
    private AttachmentService attachmentService;

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
                .reportCategory("INTERNAL")
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
        when(submissionRepository.findByRootSubmissionIdAndVersion(123L, 2)).thenReturn(Optional.empty());
        when(submissionRepository.findByParentSubmissionId(123L)).thenReturn(Optional.empty());
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

    @Test
    void reviewSubmissionRejectsSentBack() {
        Submission submission = Submission.builder().id(123L).status("AUDITOR_COMPLETED").build();
        User iqac = User.builder().role("iqac").build();
        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        assertThrows(IllegalArgumentException.class, () -> submissionService.reviewSubmission(
                123L, "SENT_BACK", "remarks", "INTERNAL", "2025-26", 1, null, null, null, iqac
        ));
    }

    @Test
    void nextCycleRejectsExternalSource() {
        Submission source = Submission.builder()
                .id(123L)
                .status("APPROVED")
                .reportCategory("EXTERNAL")
                .version(1)
                .build();
        User vc = User.builder().role("vice-chancellor").build();
        when(submissionRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(source));
        assertThrows(IllegalArgumentException.class, () -> submissionService.createNextCycle(
                123L, vc, true, 123L, 2, "EXTERNAL"
        ));
    }

    @Test
    void nextCycleRejectsNonV1Source() {
        Submission source = Submission.builder()
                .id(123L)
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .version(2)
                .build();
        User vc = User.builder().role("vice-chancellor").build();
        when(submissionRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(source));
        assertThrows(IllegalArgumentException.class, () -> submissionService.createNextCycle(
                123L, vc, true, 123L, 2, "EXTERNAL"
        ));
    }

    @Test
    void nextCyclePreventsDuplicateSuccessor() {
        Submission source = Submission.builder()
                .id(123L)
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .version(1)
                .hasNextCycle(true)
                .nextVersionId(124L)
                .build();
        User vc = User.builder().role("vice-chancellor").build();
        when(submissionRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(source));
        assertThrows(com.director_appraisal.director_appraisal.exception.ConflictException.class, () -> submissionService.createNextCycle(
                123L, vc, true, 123L, 2, "EXTERNAL"
        ));
    }

    @Test
    void iqacCanForwardToMultipleMatchingAuditors() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("director@dypiu.ac.in")
                .auditType("academic")
                .school("SOCSEA")
                .status("SUBMITTED")
                .build();
        User iqac = User.builder().id(1L).email("iqac@dypiu.ac.in").role("iqac").build();
        User auditorOne = User.builder()
                .id(4L)
                .email("one@example.com")
                .name("Auditor One")
                .role("academic-internal-auditor")
                .accountType("auditor")
                .category("academic")
                .auditorType("internal")
                .school("SOCSEA")
                .build();
        User auditorTwo = User.builder()
                .id(8L)
                .email("two@example.com")
                .name("Auditor Two")
                .role("academic-internal-auditor")
                .accountType("auditor")
                .category("academic")
                .auditorType("internal")
                .school("School of Computer Science & Applications")
                .build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(auditorAssignmentRepository.existsBySubmissionId(123L)).thenReturn(false);
        when(userRepository.findById(4L)).thenReturn(Optional.of(auditorOne));
        when(userRepository.findById(8L)).thenReturn(Optional.of(auditorTwo));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSubmission(
                123L,
                iqac,
                "UNDER_REVIEW",
                "INTERNAL",
                null,
                List.of(4L, 8L),
                null,
                null,
                null,
                null,
                null
        );

        assertEquals("UNDER_REVIEW", updated.getStatus());
        assertEquals("internal", updated.getForwardedAuditorType());
        assertNotNull(updated.getForwardedAt());
        verify(auditorAssignmentRepository, atLeastOnce()).save(any(SubmissionAuditorAssignment.class));
    }

    @Test
    void forwardingRejectsDuplicateForwarding() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("director@dypiu.ac.in")
                .auditType("academic")
                .school("SOCSEA")
                .status("UNDER_REVIEW")
                .build();
        User iqac = User.builder().id(1L).email("iqac@dypiu.ac.in").role("iqac").build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(auditorAssignmentRepository.existsBySubmissionId(123L)).thenReturn(true);

        assertThrows(com.director_appraisal.director_appraisal.exception.ConflictException.class, () -> submissionService.updateSubmission(
                123L,
                iqac,
                "UNDER_REVIEW",
                "INTERNAL",
                null,
                List.of(4L),
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void administrativePartAAndPartDAttachmentsArePreservedInTablesData() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User owner = User.builder()
                .email("iqac@dypiu.ac.in")
                .role("iqac")
                .build();
        String tablesData = "{\"scholarshipSummary\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"summary.pdf\",\"url\":\"/uploads/users/abc/attachments/summary.pdf\"}]}]," +
                "\"scholarshipStudents\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"student.pdf\",\"url\":\"/uploads/users/abc/attachments/student.pdf\"}]}]," +
                "\"hackathons\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"hackathon.pdf\",\"url\":\"/uploads/users/abc/attachments/hackathon.pdf\"}]}]," +
                "\"culturalActivities\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"cultural.pdf\",\"url\":\"/uploads/users/abc/attachments/cultural.pdf\"}]}]," +
                "\"sportsActivities\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"sports.pdf\",\"url\":\"/uploads/users/abc/attachments/sports.pdf\"}]}]," +
                "\"communityActivities\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"community.pdf\",\"url\":\"/uploads/users/abc/attachments/community.pdf\"}]}]," +
                "\"adminStudentAwards\":[{\"srNo\":\"1\",\"Attachment\":[{\"fileName\":\"award.pdf\",\"url\":\"/uploads/users/abc/attachments/award.pdf\"},{\"fileName\":\"award-extra.pdf\",\"url\":\"/uploads/users/abc/attachments/award-extra.pdf\"}]}]}";

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSubmission(
                123L,
                owner,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                tablesData,
                null
        );

        assertTrue(updated.getTablesData().contains("summary.pdf"));
        assertTrue(updated.getTablesData().contains("student.pdf"));
        assertTrue(updated.getTablesData().contains("hackathon.pdf"));
        assertTrue(updated.getTablesData().contains("cultural.pdf"));
        assertTrue(updated.getTablesData().contains("sports.pdf"));
        assertTrue(updated.getTablesData().contains("community.pdf"));
        assertTrue(updated.getTablesData().contains("award-extra.pdf"));
    }

    @Test
    void deletingTableAttachmentDeletesOnlyUnreferencedFile() throws Exception {
        Submission submission = Submission.builder()
                .id(123L)
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{}")
                .tablesData("{\"scholarshipSummary\":[{\"Attachment\":[{\"fileName\":\"old.pdf\",\"url\":\"/uploads/users/abc/attachments/old.pdf\"}," +
                        "{\"fileName\":\"kept.pdf\",\"url\":\"/uploads/users/abc/attachments/kept.pdf\"}]}]}")
                .attachments("[]")
                .build();
        User owner = User.builder()
                .email("registrar@dypiu.ac.in")
                .role("administrative")
                .post("registrar")
                .build();
        String newTablesData = "{\"scholarshipSummary\":[{\"Attachment\":[{\"fileName\":\"kept.pdf\",\"url\":\"/uploads/users/abc/attachments/kept.pdf\"}]}]}";

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSubmission(
                123L,
                owner,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                newTablesData,
                "[]"
        );

        verify(attachmentService).deleteFile("/uploads/users/abc/attachments/old.pdf");
        verify(attachmentService, never()).deleteFile("/uploads/users/abc/attachments/kept.pdf");
        assertFalse(updated.getTablesData().contains("old.pdf"));
        assertTrue(updated.getTablesData().contains("kept.pdf"));
    }

    @Test
    void administrativeTableAttachmentRejectsInvalidPdfMetadata() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User owner = User.builder()
                .email("registrar@dypiu.ac.in")
                .role("administrative")
                .post("Registrar")
                .build();
        String invalidTablesData = "{\"scholarshipSummary\":[{\"Attachment\":[{\"fileName\":\"bad.docx\",\"url\":\"/uploads/users/abc/attachments/bad.docx\"}]}]}";

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));

        assertThrows(IllegalArgumentException.class, () -> submissionService.updateSubmission(
                123L,
                owner,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                invalidTablesData,
                null
        ));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void registrarCanEditPartAAndPartCButNotPartBOrPartDOrPartE() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User registrar = User.builder()
                .email("registrar@dypiu.ac.in")
                .role("administrative")
                .post("registrar")
                .build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSubmission(
                123L,
                registrar,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"schoolName\":\"Administrative Office\"}",
                "{\"scholarshipSummary\":[{\"srNo\":\"1\"}],\"statutoryBodies\":[{\"srNo\":\"1\"}]}",
                null
        );
        assertTrue(updated.getTablesData().contains("scholarshipSummary"));
        assertTrue(updated.getTablesData().contains("statutoryBodies"));

        assertThrows(SecurityException.class, () -> submissionService.updateSubmission(
                123L,
                registrar,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"facultyInformation\":[{\"srNo\":\"1\"}]}",
                null
        ));
        assertThrows(SecurityException.class, () -> submissionService.updateSubmission(
                123L,
                registrar,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"hackathons\":[{\"srNo\":\"1\"}]}",
                null
        ));
        assertThrows(SecurityException.class, () -> submissionService.updateSubmission(
                123L,
                registrar,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"partESchools\":[{\"schoolCode\":\"SOCSEA\"}]}",
                null,
                null
        ));
    }

    @Test
    void independentAdministrativePostSubmissionProgressControlsGlobalStatus() {
        Submission submission = Submission.builder()
                .id(123L)
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"SUBMITTED\",\"dean-student-welfare\":\"SUBMITTED\",\"dean-placement\":\"SUBMITTED\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User registrar = User.builder()
                .email("registrar@dypiu.ac.in")
                .role("administrative")
                .post("registrar")
                .build();

        when(submissionRepository.findById(123L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission submitted = submissionService.updateSubmission(
                123L,
                registrar,
                "SUBMITTED",
                null,
                null,
                null,
                null,
                null,
                "{\"schoolName\":\"Administrative Office\"}",
                "{\"scholarshipSummary\":[{\"srNo\":\"1\"}]}",
                null
        );

        assertEquals("SUBMITTED", submitted.getStatus());
        assertTrue(submitted.getValuesData().contains("\"registrar\":\"SUBMITTED\""));
    }

    @Test
    void sharedAdministrativeDraftReturnsSameFormForDifferentAuthorities() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .academicYear("2025-2026")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User registrar = User.builder().role("administrative").post("registrar").build();
        User hr = User.builder().role("administrative").post("hr").build();

        when(academicYearService.getCurrentAcademicYearLabel()).thenReturn("2025-2026");
        when(submissionRepository.findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(
                "administrative.shared@dypiu.ac.in", "administrative", "2025-2026"))
                .thenReturn(Optional.of(shared));

        assertEquals(500L, submissionService.getOrCreateSharedAdministrativeDraft(registrar).getId());
        assertEquals(500L, submissionService.getOrCreateSharedAdministrativeDraft(hr).getId());
    }

    @Test
    void sharedAdministrativeRejectsUnauthorizedSectionUpdate() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User hr = User.builder().role("administrative").post("hr").build();

        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));

        assertThrows(SecurityException.class, () -> submissionService.updateSharedAdministrativeContribution(
                500L,
                hr,
                null,
                "hr",
                List.of("B"),
                null,
                "{\"scholarshipSummary\":[{\"srNo\":\"1\"}]}",
                "[]"
        ));
    }

    @Test
    void sharedAdministrativeDeanStudentWelfareCanMergeOnlySectionDAdminTables() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{\"scholarshipSummary\":[{\"srNo\":\"existing\"}]}")
                .attachments("[]")
                .build();
        User dsw = User.builder()
                .email("dsw@dypiu.ac.in")
                .role("administrative")
                .post("dean-student-welfare")
                .build();

        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSharedAdministrativeContribution(
                500L,
                dsw,
                null,
                "dean-student-welfare",
                List.of("D"),
                "{}",
                "{\"hackathons\":[{\"srNo\":\"1\"}],\"culturalActivities\":[{\"srNo\":\"2\"}],\"sportsActivities\":[{\"srNo\":\"3\"}],\"communityActivities\":[{\"srNo\":\"4\"}],\"adminStudentAwards\":[{\"srNo\":\"5\"}]}",
                "[]"
        );

        assertTrue(updated.getTablesData().contains("hackathons"));
        assertTrue(updated.getTablesData().contains("culturalActivities"));
        assertTrue(updated.getTablesData().contains("sportsActivities"));
        assertTrue(updated.getTablesData().contains("communityActivities"));
        assertTrue(updated.getTablesData().contains("adminStudentAwards"));
        assertTrue(updated.getTablesData().contains("scholarshipSummary"));
        assertFalse(updated.getTablesData().contains("studentAwards"));
        assertTrue(updated.getValuesData().contains("\"dean-student-welfare\":\"IN_PROGRESS\""));
    }

    @Test
    void sharedAdministrativeDeanStudentWelfareCannotMergeAcademicStudentAwardsTable() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User dsw = User.builder()
                .email("dsw@dypiu.ac.in")
                .role("administrative")
                .post("dean-student-welfare")
                .build();

        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));

        assertThrows(SecurityException.class, () -> submissionService.updateSharedAdministrativeContribution(
                500L,
                dsw,
                null,
                "dean-student-welfare",
                List.of("D"),
                "{}",
                "{\"studentAwards\":[{\"srNo\":\"legacy-academic\"}]}",
                "[]"
        ));
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void sharedAdministrativeApprovalMarksOnlyContributorApproved() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User registrar = User.builder()
                .id(10L)
                .email("registrar@dypiu.ac.in")
                .name("Registrar")
                .role("administrative")
                .post("registrar")
                .build();

        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Submission updated = submissionService.updateSharedAdministrativeContribution(
                500L,
                registrar,
                "APPROVE_CONTRIBUTION",
                "registrar",
                List.of("A", "C"),
                "{\"schoolName\":\"Administrative Office\"}",
                "{\"scholarshipSummary\":[{\"srNo\":\"1\"}],\"statutoryBodies\":[{\"srNo\":\"1\"}]}",
                "[]"
        );

        assertEquals("DRAFT", updated.getStatus());
        assertTrue(updated.getValuesData().contains("\"registrar\":\"APPROVED\""));
        assertTrue(updated.getValuesData().contains("\"hr\":\"DRAFT\""));
        assertTrue(updated.getValuesData().contains("\"administrativeApprovals\""));
    }

    @Test
    void testSectionPermissionEnforcement() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User hr = User.builder().email("hr@dypiu.ac.in").role("administrative").post("hr").build();
        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));

        // HR attempts to edit section A (owned by Registrar)
        assertThrows(SecurityException.class, () -> submissionService.updateSharedAdministrativeContribution(
                500L,
                hr,
                null,
                "hr",
                List.of("A"),
                "{\"universityName\":\"New Name\"}",
                "{}",
                "[]"
        ));
    }

    @Test
    void testAutomaticStatusTransitionWhenAllSubmissionsComplete() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"SUBMITTED\",\"hr\":\"SUBMITTED\",\"dean-student-welfare\":\"SUBMITTED\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User deanPlacement = User.builder().email("placement@dypiu.ac.in").role("administrative").post("dean-placement").name("Dean").build();
        when(submissionRepository.findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(anyString(), anyString(), anyString())).thenReturn(Optional.of(shared));
        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Submit the last part (dean-placement)
        Submission result = submissionService.submitAdministrativePart("2025-2026", deanPlacement);

        assertEquals("SUBMITTED", result.getStatus());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    void testLockingSubmittedSections() {
        Submission shared = Submission.builder()
                .id(500L)
                .email("administrative.shared@dypiu.ac.in")
                .auditType("administrative")
                .status("DRAFT")
                .valuesData("{\"administrativeProgress\":{\"registrar\":\"SUBMITTED\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}}")
                .tablesData("{}")
                .attachments("[]")
                .build();
        User registrar = User.builder().email("registrar@dypiu.ac.in").role("administrative").post("registrar").build();
        when(submissionRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(shared));

        // Registrar attempts to edit their already-submitted section
        assertThrows(SecurityException.class, () -> submissionService.updateSharedAdministrativeContribution(
                500L,
                registrar,
                null,
                "registrar",
                List.of("A"),
                "{\"universityName\":\"Edited\"}",
                "{}",
                "[]"
        ));
    }
}
