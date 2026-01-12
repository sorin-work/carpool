package com.carpool.rotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    /** Absences in the group that overlap [from, to). */
    @Query("""
            select a from Absence a join fetch a.member m join fetch m.user
            where m.group.id = :groupId and a.endsOn >= :from and a.startsOn < :to
            order by a.startsOn, a.id
            """)
    List<Absence> findOverlapping(Long groupId, LocalDate from, LocalDate to);
}
