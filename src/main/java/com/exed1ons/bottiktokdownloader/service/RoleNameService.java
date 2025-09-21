package com.exed1ons.bottiktokdownloader.service;

import com.exed1ons.bottiktokdownloader.dao.model.Role;
import com.exed1ons.bottiktokdownloader.dao.repository.RoleNameRepository;
import jakarta.annotation.Nonnull;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleNameService {

    private final RoleNameRepository roleNameRepository;

    public void addRole(String name) {
        roleNameRepository.save(Role.builder()
                .name(name)
                .build());
    }

    public Optional<Role> removeRoleByName(@Nonnull final String name) {
        return roleNameRepository.removeByName(name);
    }
}