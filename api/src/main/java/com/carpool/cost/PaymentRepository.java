package com.carpool.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select p from Payment p join fetch p.from f join fetch f.user join fetch p.to t join fetch t.user
            where p.group.id = :groupId and p.paidOn >= :from and p.paidOn < :to
            order by p.paidOn, p.id
            """)
    List<Payment> findInPeriod(Long groupId, LocalDate from, LocalDate to);
}
