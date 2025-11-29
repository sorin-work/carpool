package com.carpool.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("select m from GroupMember m join fetch m.user where m.group.id = :groupId order by m.joinedAt, m.id")
    List<GroupMember> findAllInGroup(Long groupId);

    @Query("select m from GroupMember m join fetch m.group where m.user.id = :userId order by m.group.name")
    List<GroupMember> findAllForUser(Long userId);

    long countByGroupId(Long groupId);
}
