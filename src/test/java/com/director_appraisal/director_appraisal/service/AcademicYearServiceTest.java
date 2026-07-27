package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.AcademicYear;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.AcademicYearRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private AcademicYearService academicYearService;

    @Test
    void startsNextAcademicYearAndCreatesBlankV1Forms() {
        AcademicYear current = AcademicYear.builder()
                .id(1L)
                .yearLabel("2025-2026")
                .active(true)
                .build();
        User iqac = User.builder().role("iqac").build();
        User director = User.builder()
                .email("director@dypiu.ac.in")
                .name("Director")
                .role("director")
                .school("SOCSEA")
                .build();
        User registrar = User.builder()
                .email("registrar@dypiu.ac.in")
                .name("Registrar")
                .role("administrative")
                .post("registrar")
                .build();

        when(academicYearRepository.findByActiveTrue()).thenReturn(List.of(current));
        when(academicYearRepository.findByYearLabel("2026-2027")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of(director, registrar));
        when(submissionRepository.findByEmailAndAuditTypeAndAcademicYearAndVersion(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("administrative".equals(saved.getAuditType()) ? 2L : 1L);
            }
            return saved;
        });
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = academicYearService.startNextAcademicYear(
                "2025-2026",
                "2026-2027",
                true,
                true,
                iqac
        );

        assertEquals("2026-2027", response.get("academicYear"));
        assertEquals("2025-2026", response.get("previousAcademicYear"));
        assertEquals(1, response.get("academicFormsCreated"));
        assertEquals(1, response.get("administrativeFormsCreated"));
        verify(submissionRepository, atLeast(4)).save(any(Submission.class));
    }

    @Test
    void idempotentNextAcademicYearReuse() {
        AcademicYear current = AcademicYear.builder()
                .id(1L)
                .yearLabel("2025-2026")
                .active(true)
                .build();
        AcademicYear nextYearObj = AcademicYear.builder()
                .id(2L)
                .yearLabel("2026-2027")
                .active(false)
                .build();
        User vc = User.builder().role("vice-chancellor").build();

        when(academicYearRepository.findByActiveTrue()).thenReturn(List.of(current));
        when(academicYearRepository.findByYearLabel("2026-2027")).thenReturn(Optional.of(nextYearObj));
        when(userRepository.findAll()).thenReturn(List.of());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = academicYearService.startNextAcademicYear(
                "2025-2026",
                "2026-2027",
                true,
                true,
                vc
        );

        assertEquals("2026-2027", response.get("academicYear"));
        assertTrue(nextYearObj.getActive());
    }

    @Test
    void handlesNoActiveAcademicYearsByActivatingCurrent() {
        User iqac = User.builder().role("iqac").build();

        when(academicYearRepository.findByActiveTrue()).thenReturn(List.of());
        when(academicYearRepository.findByYearLabel("2025-2026")).thenReturn(Optional.empty());
        when(academicYearRepository.findByYearLabel("2026-2027")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = academicYearService.startNextAcademicYear(
                "2025-2026",
                "2026-2027",
                true,
                true,
                iqac
        );

        assertEquals("2026-2027", response.get("academicYear"));
    }

    @Test
    void rejectsInvalidAcademicYearIncrement() {
        User iqac = User.builder().role("iqac").build();

        assertThrows(IllegalArgumentException.class, () -> academicYearService.startNextAcademicYear(
                "2025-2026",
                "2027-2028",
                true,
                true,
                iqac
        ));
    }
}
