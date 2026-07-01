package com.director_appraisal.director_appraisal;

import com.director_appraisal.director_appraisal.controller.SubmissionController;
import com.director_appraisal.director_appraisal.exception.ConflictException;
import com.director_appraisal.director_appraisal.exception.NotFoundException;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import com.director_appraisal.director_appraisal.service.AttachmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
public class SubmissionIntegrationTest {

    @Autowired
    private SubmissionController submissionController;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AttachmentService attachmentService;

    private User iqacUser;
    private User vcUser;
    private User directorUser;

    @BeforeEach
    void setUp() throws IOException {
        submissionRepository.deleteAll();
        userRepository.deleteAll();

        iqacUser = User.builder()
                .email("iqac@dypiu.ac.in")
                .name("IQAC Officer")
                .role("iqac")
                .accountType("staff")
                .status("active")
                .password("password")
                .build();
        iqacUser = userRepository.save(iqacUser);

        vcUser = User.builder()
                .email("vc@dypiu.ac.in")
                .name("Vice Chancellor")
                .role("vice-chancellor")
                .accountType("staff")
                .status("active")
                .password("password")
                .build();
        vcUser = userRepository.save(vcUser);

        directorUser = User.builder()
                .email("director@dypiu.ac.in")
                .name("Director CSE")
                .role("director")
                .school("SOCSEA")
                .accountType("director")
                .status("active")
                .password("password")
                .build();
        directorUser = userRepository.save(directorUser);

        // Setup mock attachment streaming
        when(attachmentService.downloadAttachmentStream(anyString()))
                .thenAnswer(invocation -> new ByteArrayInputStream("dummy file content".getBytes()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testIqacVcZipDownload() throws Exception {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .auditCycle("2025-26")
                .attachments("[{\"fileName\":\"proof.pdf\",\"url\":\"/uploads/users/123/attachments/proof.pdf\"}]")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        assertEquals(200, response.getStatus());
        assertEquals("application/zip", response.getContentType());
        assertNotNull(response.getHeader("Content-Disposition"));
        assertTrue(response.getHeader("Content-Disposition").contains("Academic_SOCSEA_2025-26.zip"));

        // Validate ZIP structure
        byte[] bytes = response.getContentAsByteArray();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("Other-Attachments/proof.pdf", entry.getName());
        }
    }

    @Test
    void testAuditorDirectorDownloadRejection() {
        authenticateAs(directorUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"proof.pdf\",\"url\":\"/uploads/users/123/attachments/proof.pdf\"}]")
                .build();
        sub = submissionRepository.save(sub);

        final Long subId = sub.getId();
        assertThrows(RuntimeException.class, () -> {
            submissionController.downloadAttachments(subId, new MockHttpServletResponse());
        });
    }

    @Test
    void testAcademicAndAdministrativeZipFolderOrganization() throws Exception {
        authenticateAs(iqacUser);

        // 1. Academic submission folders
        Submission acadSub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"a.pdf\",\"url\":\"/uploads/a.pdf\",\"sectionId\":\"part-a\"}," +
                        "{\"fileName\":\"b.pdf\",\"url\":\"/uploads/b.pdf\",\"sectionId\":\"part-b\"}," +
                        "{\"fileName\":\"c.pdf\",\"url\":\"/uploads/c.pdf\",\"sectionId\":\"part-c\"}," +
                        "{\"fileName\":\"d.pdf\",\"url\":\"/uploads/d.pdf\",\"sectionId\":\"part-d\"}," +
                        "{\"fileName\":\"other.pdf\",\"url\":\"/uploads/other.pdf\",\"sectionId\":\"none\"}]")
                .build();
        acadSub = submissionRepository.save(acadSub);

        MockHttpServletResponse acadResponse = new MockHttpServletResponse();
        submissionController.downloadAttachments(acadSub.getId(), acadResponse);
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(acadResponse.getContentAsByteArray()))) {
            int count = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                count++;
                String name = entry.getName();
                assertTrue(name.startsWith("Part-A/") || name.startsWith("Part-B/") ||
                        name.startsWith("Part-C/") || name.startsWith("Part-D/") ||
                        name.startsWith("Other-Attachments/"));
            }
            assertEquals(5, count);
        }

        // 2. Administrative submission folders
        Submission adminSub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("administrative")
                .school("AdminOffice")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"sa.pdf\",\"url\":\"/uploads/sa.pdf\",\"sectionId\":\"section-a\"}," +
                        "{\"fileName\":\"sb.pdf\",\"url\":\"/uploads/sb.pdf\",\"sectionId\":\"section-b\"}," +
                        "{\"fileName\":\"sc.pdf\",\"url\":\"/uploads/sc.pdf\",\"sectionId\":\"section-c\"}," +
                        "{\"fileName\":\"other.pdf\",\"url\":\"/uploads/other.pdf\",\"sectionId\":\"none\"}]")
                .build();
        adminSub = submissionRepository.save(adminSub);

        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        submissionController.downloadAttachments(adminSub.getId(), adminResponse);
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(adminResponse.getContentAsByteArray()))) {
            int count = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                count++;
                String name = entry.getName();
                assertTrue(name.startsWith("Section-A/") || name.startsWith("Section-B/") ||
                        name.startsWith("Section-C/") || name.startsWith("Other-Attachments/"));
            }
            assertEquals(4, count);
        }
    }

    @Test
    void testDuplicateAndUnsafeFilenames() throws Exception {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"duplicate.pdf\",\"url\":\"/uploads/1.pdf\"}," +
                        "{\"fileName\":\"duplicate.pdf\",\"url\":\"/uploads/2.pdf\"}," +
                        "{\"fileName\":\"../../unsafe.pdf\",\"url\":\"/uploads/3.pdf\"}]")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            boolean foundDuplicate1 = false;
            boolean foundDuplicate2 = false;
            boolean foundSafe = false;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("Other-Attachments/duplicate.pdf")) foundDuplicate1 = true;
                if (name.equals("Other-Attachments/duplicate_1.pdf")) foundDuplicate2 = true;
                if (name.equals("Other-Attachments/unsafe.pdf")) foundSafe = true;
            }
            assertTrue(foundDuplicate1);
            assertTrue(foundDuplicate2);
            assertTrue(foundSafe);
        }
    }

    @Test
    void testMissingAttachmentHandling() throws Exception {
        authenticateAs(iqacUser);

        // Mock downloadAttachmentStream to throw exception for a specific url
        when(attachmentService.downloadAttachmentStream("/uploads/missing.pdf"))
                .thenThrow(new IOException("File not found"));

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"missing.pdf\",\"url\":\"/uploads/missing.pdf\"}," +
                        "{\"fileName\":\"present.pdf\",\"url\":\"/uploads/present.pdf\"}]")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            boolean foundPresent = false;
            boolean foundMissingLog = false;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("Other-Attachments/present.pdf")) foundPresent = true;
                if (name.equals("missing-files.txt")) foundMissingLog = true;
            }
            assertTrue(foundPresent);
            assertTrue(foundMissingLog);
        }
    }

    @Test
    void testVersionSpecificAttachmentIsolation() throws Exception {
        authenticateAs(iqacUser);

        Submission v1 = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"v1_file.pdf\",\"url\":\"/uploads/v1.pdf\"}]")
                .version(1)
                .build();
        v1 = submissionRepository.save(v1);

        Submission v2 = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("EXTERNAL")
                .attachments("[{\"fileName\":\"v2_file.pdf\",\"url\":\"/uploads/v2.pdf\"}]")
                .version(2)
                .build();
        v2 = submissionRepository.save(v2);

        // V1 download
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        submissionController.downloadAttachments(v1.getId(), res1);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(res1.getContentAsByteArray()))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("Other-Attachments/v1_file.pdf", entry.getName());
            assertNull(zis.getNextEntry());
        }

        // V2 download
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        submissionController.downloadAttachments(v2.getId(), res2);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(res2.getContentAsByteArray()))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("Other-Attachments/v2_file.pdf", entry.getName());
            assertNull(zis.getNextEntry());
        }
    }

    @Test
    void testAttachmentInMetadataAndTablesDataIsDownloadedOnce() throws Exception {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[{\"fileName\":\"proof.pdf\",\"url\":\"/uploads/shared.pdf\"}]")
                .tablesData("{\"studentStrength\":[{\"proof\":{\"fileName\":\"proof.pdf\",\"url\":\"/uploads/shared.pdf\"}}]}")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            while (zis.getNextEntry() != null) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    void testZipFallsBackToTablesDataWhenSubmissionAttachmentsAreEmpty() throws Exception {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[]")
                .tablesData("{\"studentStrength\":[{\"proof\":{\"fileName\":\"table-proof.pdf\",\"url\":\"/uploads/table-proof.pdf\"}}]}")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("Other-Attachments/table-proof.pdf", entry.getName());
            assertNull(zis.getNextEntry());
        }
    }

    @Test
    void testAdministrativePartAAndPartDAttachmentsAreDiscoveredAndDeduplicatedInZip() throws Exception {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email("registrar@dypiu.ac.in")
                .auditType("administrative")
                .administrativePost("registrar")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .attachments("[]")
                .tablesData("{\"scholarshipSummary\":[{\"Attachment\":[{\"fileName\":\"summary.pdf\",\"url\":\"/uploads/summary.pdf\"}]}]," +
                        "\"scholarshipStudents\":[{\"Attachment\":[{\"fileName\":\"student.pdf\",\"url\":\"/uploads/student.pdf\"}]}]," +
                        "\"hackathons\":[{\"Attachment\":[{\"fileName\":\"hackathon.pdf\",\"url\":\"/uploads/hackathon.pdf\"},{\"fileName\":\"shared.pdf\",\"url\":\"/uploads/shared-admin.pdf\"}]}]," +
                        "\"culturalActivities\":[{\"Attachment\":[{\"fileName\":\"cultural.pdf\",\"url\":\"/uploads/cultural.pdf\"}]}]," +
                        "\"sportsActivities\":[{\"Attachment\":[{\"fileName\":\"sports.pdf\",\"url\":\"/uploads/sports.pdf\"}]}]," +
                        "\"communityActivities\":[{\"Attachment\":[{\"fileName\":\"community.pdf\",\"url\":\"/uploads/community.pdf\"}]}]," +
                        "\"adminStudentAwards\":[{\"Attachment\":[{\"fileName\":\"award.pdf\",\"url\":\"/uploads/award.pdf\"},{\"fileName\":\"shared.pdf\",\"url\":\"/uploads/shared-admin.pdf\"}]}]}")
                .build();
        sub = submissionRepository.save(sub);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(sub.getId(), response);

        List<String> entryNames = new java.util.ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }

        assertTrue(entryNames.contains("Registrar/Part-A/summary.pdf"));
        assertTrue(entryNames.contains("Registrar/Part-A/student.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/hackathon.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/cultural.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/sports.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/community.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/award.pdf"));
        assertTrue(entryNames.contains("Dean-Student-Welfare/Part-D/shared.pdf"));
        assertEquals(8, entryNames.size());
    }

    @Test
    void testRejectingNextCycleFromExternalReports() {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("EXTERNAL")
                .version(1)
                .build();
        sub = submissionRepository.save(sub);

        SubmissionController.NextCycleRequest req = new SubmissionController.NextCycleRequest();
        req.setNextVersion(2);
        req.setNextAuditorType("EXTERNAL");

        final Long subId = sub.getId();
        assertThrows(IllegalArgumentException.class, () -> {
            submissionController.createNextCycle(subId, req);
        });
    }

    @Test
    void testRejectingVersionsOtherThanV1() {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .version(2)
                .build();
        sub = submissionRepository.save(sub);

        SubmissionController.NextCycleRequest req = new SubmissionController.NextCycleRequest();
        req.setNextVersion(3);
        req.setNextAuditorType("EXTERNAL");

        final Long subId = sub.getId();
        assertThrows(IllegalArgumentException.class, () -> {
            submissionController.createNextCycle(subId, req);
        });
    }

    @Test
    void testPreventingDuplicateSuccessors() {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("INTERNAL")
                .version(1)
                .build();
        sub = submissionRepository.save(sub);

        SubmissionController.NextCycleRequest req = new SubmissionController.NextCycleRequest();
        req.setNextVersion(2);
        req.setNextAuditorType("EXTERNAL");

        submissionController.createNextCycle(sub.getId(), req);

        // Try creating next cycle again (must throw ConflictException)
        final Long subId = sub.getId();
        assertThrows(ConflictException.class, () -> {
            submissionController.createNextCycle(subId, req);
        });
    }

    @Test
    void testReturningCorrectReportGroupingMetadata() {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("APPROVED")
                .reportCategory("internal")
                .version(1)
                .hasNextCycle(false)
                .build();
        final Submission savedSub = submissionRepository.save(sub);

        List<Submission> list = submissionController.getAllSubmissions().getBody();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        Submission returned = list.stream()
                .filter(s -> s.getId().equals(savedSub.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals("ACADEMIC", returned.getAuditTypeForJson());
        assertEquals("INTERNAL", returned.getReportCategoryForJson());
        assertFalse(returned.getHasNextCycleForJson());
        assertNull(returned.getNextVersionId());
    }

    @Test
    void testRemovingIqacVcSentBackTransitions() {
        authenticateAs(iqacUser);

        Submission sub = Submission.builder()
                .email(directorUser.getEmail())
                .auditType("academic")
                .school("SOCSEA")
                .status("AUDITOR_COMPLETED")
                .version(1)
                .build();
        sub = submissionRepository.save(sub);

        SubmissionController.ReviewRequest req = new SubmissionController.ReviewRequest();
        req.setStatus("SENT_BACK");
        req.setRemarks("Need corrections");

        final Long subId = sub.getId();
        assertThrows(IllegalArgumentException.class, () -> {
            submissionController.reviewSubmission(subId, req);
        });
    }

    @Test
    void testAdministrativeSaveLoadAndSubmitPartEWithB2B3B4Data() throws Exception {
        User placement = User.builder()
                .email("placement@dypiu.ac.in")
                .name("Dean Placement")
                .role("administrative")
                .post("dean-placement")
                .accountType("user")
                .status("active")
                .password("password")
                .build();
        userRepository.save(placement);

        authenticateAs(placement);

        String partEJson = "{\"partESchools\":[{"
                + "\"schoolCode\":\"SOCSEA\",\"schoolName\":\"School of Computer Science\","
                + "\"placementPrograms\":[],"
                + "\"activities\":[{"
                + "  \"Sr No\":\"1\",\"Activity\":\"Internship\","
                + "  \"All Students List Attachment\":[{\"name\":\"list.pdf\",\"url\":\"/uploads/list.pdf\"}],"
                + "  \"Certificates Attachment\":[{\"name\":\"certs.pdf\",\"url\":\"/uploads/certs.pdf\"}]"
                + "}],"
                + "\"careerGuidanceActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Guidance Session\","
                + "  \"Link for Proofs\":[{\"name\":\"guidance.pdf\",\"url\":\"/uploads/guidance.pdf\"}]"
                + "}],"
                + "\"industryInteractionActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Interaction Session\","
                + "  \"Link for Proofs\":[{\"name\":\"interaction.pdf\",\"url\":\"/uploads/interaction.pdf\"}]"
                + "}],"
                + "\"otherActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Other Session\","
                + "  \"Link for Proofs\":[{\"name\":\"other.pdf\",\"url\":\"/uploads/other.pdf\"}]"
                + "}]"
                + "}]}";

        SubmissionController.FormSubmissionRequest saveReq = new SubmissionController.FormSubmissionRequest();
        saveReq.setAuditType("administrative");
        saveReq.setSharedAdministrativeForm(true);
        saveReq.setContributorPost("dean-placement");
        saveReq.setSections(List.of("E"));
        saveReq.setValuesData(partEJson);
        saveReq.setTablesData("{}");
        saveReq.setAttachments("[]");

        // 1. Save draft
        Submission draft = submissionController.saveDraft(saveReq).getBody();
        assertNotNull(draft);
        assertTrue(draft.getValuesData().contains("careerGuidanceActivities"));
        assertTrue(draft.getValuesData().contains("industryInteractionActivities"));
        assertTrue(draft.getValuesData().contains("otherActivities"));
        assertTrue(draft.getValuesData().contains("guidance.pdf"));

        // 2. Fetch/Load draft
        Submission fetched = submissionController.getMyDraft("administrative", true).getBody();
        assertNotNull(fetched);
        assertTrue(fetched.getValuesData().contains("careerGuidanceActivities"));
        assertTrue(fetched.getValuesData().contains("industryInteractionActivities"));
        assertTrue(fetched.getValuesData().contains("otherActivities"));
        assertTrue(fetched.getValuesData().contains("guidance.pdf"));

        // 3. Submit
        SubmissionController.FormSubmissionRequest submitReq = new SubmissionController.FormSubmissionRequest();
        submitReq.setAuditType("administrative");
        submitReq.setSharedAdministrativeForm(true);
        submitReq.setContributorPost("dean-placement");
        submitReq.setSections(List.of("E"));
        submitReq.setValuesData(partEJson);
        submitReq.setTablesData("{}");
        submitReq.setAttachments("[]");

        Submission submitted = submissionController.submitForm(submitReq).getBody();
        assertNotNull(submitted);
        assertTrue(submitted.getValuesData().contains("careerGuidanceActivities"));
        assertTrue(submitted.getValuesData().contains("industryInteractionActivities"));
        assertTrue(submitted.getValuesData().contains("otherActivities"));
        assertTrue(submitted.getValuesData().contains("guidance.pdf"));
    }

    @Test
    void testAttachmentExtractionIncludesLinkForProofs() throws Exception {
        User placement = User.builder()
                .email("placement@dypiu.ac.in")
                .name("Dean Placement")
                .role("administrative")
                .post("dean-placement")
                .accountType("user")
                .status("active")
                .password("password")
                .build();
        userRepository.save(placement);

        authenticateAs(placement);

        String partEJson = "{\"partESchools\":[{"
                + "\"schoolCode\":\"SOCSEA\",\"schoolName\":\"School of Computer Science\","
                + "\"careerGuidanceActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Guidance Session\","
                + "  \"Link for Proofs\":[{\"name\":\"guidance.pdf\",\"url\":\"/uploads/guidance.pdf\"}]"
                + "}],"
                + "\"industryInteractionActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Interaction Session\","
                + "  \"Link for Proofs\":[{\"name\":\"interaction.pdf\",\"url\":\"/uploads/interaction.pdf\"}]"
                + "}],"
                + "\"otherActivities\":[{"
                + "  \"Sr No\":\"1\",\"Title of the Session\":\"Other Session\","
                + "  \"Link for Proofs\":[{\"name\":\"other.pdf\",\"url\":\"/uploads/other.pdf\"}]"
                + "}]"
                + "}]}";

        SubmissionController.FormSubmissionRequest saveReq = new SubmissionController.FormSubmissionRequest();
        saveReq.setAuditType("administrative");
        saveReq.setSharedAdministrativeForm(true);
        saveReq.setContributorPost("dean-placement");
        saveReq.setSections(List.of("E"));
        saveReq.setValuesData(partEJson);
        saveReq.setTablesData("{}");
        saveReq.setAttachments("[]");

        Submission submitted = submissionController.submitForm(saveReq).getBody();
        assertNotNull(submitted);
        submitted.setStatus("SUBMITTED");
        submitted = submissionRepository.save(submitted);

        // Authenticate as IQAC to download ZIP
        authenticateAs(iqacUser);

        MockHttpServletResponse response = new MockHttpServletResponse();
        submissionController.downloadAttachments(submitted.getId(), true, response);

        byte[] zipBytes = response.getContentAsByteArray();
        assertTrue(zipBytes.length > 0);

        java.util.Set<String> entryNames = new java.util.HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }

        assertTrue(entryNames.stream().anyMatch(name -> name.contains("guidance.pdf")));
        assertTrue(entryNames.stream().anyMatch(name -> name.contains("interaction.pdf")));
        assertTrue(entryNames.stream().anyMatch(name -> name.contains("other.pdf")));
    }
}
