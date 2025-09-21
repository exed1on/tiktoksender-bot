package com.exed1ons.bottiktokdownloader.service;

import com.exed1ons.bottiktokdownloader.dao.model.GroupMember;
import com.exed1ons.bottiktokdownloader.dao.repository.GroupMemberRepository;
import com.exed1ons.bottiktokdownloader.dao.repository.RoleNameRepository;
import jakarta.annotation.Nonnull;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;

    private final RoleNameRepository roleNameRepository;

    public void addMember(Long id, String userName, String firstName) {
        groupMemberRepository.save(GroupMember.builder()
                .id(id)
                .userName(userName)
                .firstName(firstName)
                .build());
    }


    public List<GroupMember> findByRoleName(@Nonnull String roleName) {
        return groupMemberRepository.findByRoleName(roleName);
    }


    public Optional<GroupMember> findById(Long id) {
        return groupMemberRepository.findById(id);
    }


    public List<GroupMember> findAll() {
        return groupMemberRepository.findAll();
    }


    public Optional<GroupMember> findByUserName(String username) {
        return groupMemberRepository.findByUserName(username);
    }


    public void assignRoleToMember(@Nonnull final Long memberId, @Nonnull final String roleName) {
        var member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        var role = roleNameRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));


        member.getRoles().add(role);
        groupMemberRepository.save(member);
    }


    public void removeRoleFromMember(@Nonnull final Long memberId, @Nonnull final String roleName) {
        var member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        var role = roleNameRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        member.getRoles().remove(role);
        groupMemberRepository.save(member);
    }
}