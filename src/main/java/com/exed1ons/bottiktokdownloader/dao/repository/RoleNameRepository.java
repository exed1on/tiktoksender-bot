package com.exed1ons.bottiktokdownloader.dao.repository;

import com.exed1ons.bottiktokdownloader.dao.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleNameRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> removeByName(String name);

}