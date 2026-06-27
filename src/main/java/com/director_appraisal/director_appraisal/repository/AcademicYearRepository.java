package com.director_appraisal.director_appraisal.repository;

import com.director_appraisal.director_appraisal.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByYearLabel(String yearLabel);
    List<AcademicYear> findByActiveTrue();
    boolean existsByYearLabel(String yearLabel);
}
