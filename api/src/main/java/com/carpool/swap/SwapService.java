package com.carpool.swap;

import com.carpool.booking.SeatPlanner;
import com.carpool.common.ApiException;
import com.carpool.group.GroupAccess;
import com.carpool.group.GroupMember;
import com.carpool.group.GroupMemberRepository;
import com.carpool.notification.NotificationService;
import com.carpool.notification.NotificationType;
import com.carpool.ride.Ride;
import com.carpool.ride.RideMessages;
import com.carpool.ride.RideRepository;
import com.carpool.ride.RideRules;
import com.carpool.swap.dto.CreateSwapRequest;
import com.carpool.swap.dto.SwapResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class SwapService {

    private final SwapRepository swaps;
    private final RideRepository rides;
    private final GroupMemberRepository members;
    private final GroupAccess access;
    private final NotificationService notifications;
    private final SeatPlanner planner;
    private final Clock clock;

    public SwapService(SwapRepository swaps, RideRepository rides, GroupMemberRepository members, GroupAccess access,
                       NotificationService notifications, SeatPlanner planner, Clock clock) {
        this.planner = planner;
        this.swaps = swaps;
        this.rides = rides;
        this.members = members;
        this.access = access;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public SwapResponse create(Long rideId, Long userId, CreateSwapRequest request) {
        Ride ride = rides.findWithDetails(rideId).orElseThrow(() -> ApiException.notFound("Ride"));
        Long groupId = ride.getTrip().getGroup().getId();
        GroupMember me = access.requireMember(groupId, userId);
        if (!ride.isDrivenBy(me)) {
            throw ApiException.forbidden("Only the driver can ask for a swap");
        }
        requireOpen(ride);
        GroupMember target = members.findById(request.targetMemberId())
                .filter(m -> m.getGroup().getId().equals(groupId))
                .orElseThrow(() -> ApiException.notFound("Member"));
        if (target.getId().equals(me.getId())) {
            throw ApiException.badRequest("Pick someone else to swap with");
        }
        if (!target.isCanDrive()) {
            throw ApiException.badRequest(target.getDisplayName() + " has no car set up");
        }
        Ride offered = null;
        if (request.offeredRideId() != null) {
            offered = rides.findWithDetails(request.offeredRideId())
                    .filter(r -> r.getTrip().getGroup().getId().equals(groupId))
                    .orElseThrow(() -> ApiException.notFound("Offered ride"));
            if (!offered.isDrivenBy(target)) {
                throw ApiException.badRequest(target.getDisplayName() + " is not driving the ride you offered to take");
            }
            requireOpen(offered);
        }
        if (swaps.existsByRideIdAndTargetIdAndStatus(rideId, target.getId(), SwapStatus.PENDING)) {
            throw ApiException.conflict("You already asked " + target.getDisplayName() + " about this ride");
        }
        String note = request.message() == null || request.message().isBlank() ? null : request.message().trim();
        SwapRequest swap = swaps.save(new SwapRequest(ride, me, target, offered, note, clock.instant()));

        String message = RideMessages.bold(me.getDisplayName()) + " asks if you can drive " + RideMessages.describe(ride)
                + (offered != null ? ", taking " + RideMessages.describe(offered) + " from you in return" : "") + ".";
        notifications.notify(target.getUser(), NotificationType.SWAP_REQUESTED, message, "/groups/" + groupId + "/swaps");
        return SwapResponse.from(swap);
    }

    @Transactional(readOnly = true)
    public List<SwapResponse> list(Long groupId, Long userId) {
        GroupMember me = access.requireMember(groupId, userId);
        return swaps.findInvolving(groupId, me.getId()).stream().map(SwapResponse::from).toList();
    }

    @Transactional
    public SwapResponse accept(Long swapId, Long userId) {
        SwapRequest swap = load(swapId);
        Long groupId = swap.getRide().getTrip().getGroup().getId();
        access.requireMember(groupId, userId);
        requirePending(swap);
        Ride ride = swap.getRide();
        GroupMember requester = swap.getRequester();
        GroupMember target = swap.getTarget();
        if (!ride.isDrivenBy(requester)) {
            throw ApiException.conflict("The ride has changed since the swap was requested");
        }
        requireOpen(ride);
        Ride offered = swap.getOfferedRide();
        if (offered != null) {
            if (!offered.isDrivenBy(target)) {
                throw ApiException.conflict("The ride you would hand over has changed");
            }
            requireOpen(offered);
        }

        ride.assignDriver(target, target.getCarSeats());
        planner.rebalance(ride, target);
        if (offered != null) {
            offered.assignDriver(requester, requester.getCarSeats());
            planner.rebalance(offered, requester);
        }
        swap.resolve(SwapStatus.ACCEPTED, clock.instant());
        swaps.findPendingForRide(ride.getId()).stream()
                .filter(other -> !other.getId().equals(swap.getId()))
                .forEach(other -> other.resolve(SwapStatus.CANCELLED, clock.instant()));

        notifications.notify(requester.getUser(), NotificationType.SWAP_ACCEPTED,
                RideMessages.bold(target.getDisplayName()) + " will drive " + RideMessages.describe(ride) + ".",
                RideMessages.link(ride));
        return SwapResponse.from(swap);
    }

    @Transactional
    public SwapResponse decline(Long swapId, Long userId) {
        SwapRequest swap = load(swapId);
        GroupMember me = access.requireMember(swap.getRide().getTrip().getGroup().getId(), userId);
        if (!swap.getTarget().getId().equals(me.getId())) {
            throw ApiException.forbidden("Only " + swap.getTarget().getDisplayName() + " can answer this swap");
        }
        requirePending(swap);
        swap.resolve(SwapStatus.DECLINED, clock.instant());
        notifications.notify(swap.getRequester().getUser(), NotificationType.SWAP_DECLINED,
                RideMessages.bold(me.getDisplayName()) + " cannot drive " + RideMessages.describe(swap.getRide()) + ".",
                RideMessages.link(swap.getRide()));
        return SwapResponse.from(swap);
    }

    @Transactional
    public SwapResponse cancel(Long swapId, Long userId) {
        SwapRequest swap = load(swapId);
        GroupMember me = access.requireMember(swap.getRide().getTrip().getGroup().getId(), userId);
        if (!swap.getRequester().getId().equals(me.getId())) {
            throw ApiException.forbidden("Only the driver who asked can withdraw a swap");
        }
        requirePending(swap);
        swap.resolve(SwapStatus.CANCELLED, clock.instant());
        return SwapResponse.from(swap);
    }

    private SwapRequest load(Long swapId) {
        return swaps.findWithDetails(swapId).orElseThrow(() -> ApiException.notFound("Swap"));
    }

    private static void requirePending(SwapRequest swap) {
        if (!swap.isPending()) {
            throw ApiException.conflict("This swap was already " + swap.getStatus().name().toLowerCase());
        }
    }

    private void requireOpen(Ride ride) {
        RideRules.requireOpen(ride, clock.instant());
    }
}
