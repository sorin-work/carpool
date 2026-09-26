package com.carpool.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<CarpoolGroup, Long> {

    Optional<CarpoolGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    /** Members, trips, rides and everything below them go with the group (on delete cascade). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CarpoolGroup g where g.id = :id")
    void deleteWithEverything(Long id);
}
