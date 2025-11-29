package com.carpool.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<CarpoolGroup, Long> {

    Optional<CarpoolGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
