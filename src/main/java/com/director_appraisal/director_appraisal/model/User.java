package com.director_appraisal.director_appraisal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;
    private String designation;
    private String school;
    private String role; // e.g. director, administrative, vice-chancellor, iqac

    private String accountType;
    private String category;
    private String auditorType;
    private String auditorRole;
    private String post;
    private String schools;

    public List<String> getSchoolsList() {
        if (schools == null || schools.isBlank()) {
            return java.util.List.of();
        }
        return java.util.List.of(schools.split(","));
    }

    public void setSchoolsList(List<String> list) {
        if (list == null || list.isEmpty()) {
            this.schools = null;
        } else {
            this.schools = String.join(",", list);
        }
    }

    @Builder.Default
    private String status = "active";

    @Builder.Default
    private Boolean deleted = false;

    private java.time.LocalDateTime deletedAt;

    private String deletedBy;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
