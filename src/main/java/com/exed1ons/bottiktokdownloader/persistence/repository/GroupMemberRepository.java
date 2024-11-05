package com.exed1ons.bottiktokdownloader.persistence.repository;

import com.exed1ons.bottiktokdownloader.persistence.entity.GroupMember;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("SELECT gm FROM GroupMember gm JOIN gm.roles r WHERE r.name = :roleName")
    List<GroupMember> findByRoleName(@Nonnull String roleName);

    Optional<GroupMember> findByUserName(String username);
}
