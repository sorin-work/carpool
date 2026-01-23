package com.carpool.swap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SwapRepository extends JpaRepository<SwapRequest, Long> {

    @Query("""
            select s from SwapRequest s
            join fetch s.ride r join fetch r.trip t join fetch t.group
            join fetch s.requester rq join fetch rq.user
            join fetch s.target tg join fetch tg.user
            left join fetch s.offeredRide o left join fetch o.trip
            where s.id = :id
            """)
    Optional<SwapRequest> findWithDetails(Long id);

    @Query("""
            select s from SwapRequest s
            join fetch s.ride r join fetch r.trip t
            join fetch s.requester rq join fetch rq.user
            join fetch s.target tg join fetch tg.user
            left join fetch s.offeredRide o left join fetch o.trip
            where t.group.id = :groupId and (rq.id = :memberId or tg.id = :memberId)
            order by s.createdAt desc, s.id desc
            """)
    List<SwapRequest> findInvolving(Long groupId, Long memberId);

    boolean existsByRideIdAndTargetIdAndStatus(Long rideId, Long targetId, SwapStatus status);

    @Query("select s from SwapRequest s where s.ride.id = :rideId and s.status = com.carpool.swap.SwapStatus.PENDING")
    List<SwapRequest> findPendingForRide(Long rideId);
}
