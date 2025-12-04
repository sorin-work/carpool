package com.carpool.group;

import com.carpool.common.ApiException;
import com.carpool.group.dto.CreateGroupRequest;
import com.carpool.group.dto.GroupDetail;
import com.carpool.group.dto.GroupSummary;
import com.carpool.group.dto.MemberResponse;
import com.carpool.group.dto.UpdateGroupRequest;
import com.carpool.group.dto.UpdateMembershipRequest;
import com.carpool.user.User;
import com.carpool.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
public class GroupService {

    static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Bucharest");

    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final UserRepository users;
    private final GroupAccess access;
    private final Clock clock;

    public GroupService(GroupRepository groups, GroupMemberRepository members, UserRepository users,
                        GroupAccess access, Clock clock) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public GroupDetail create(Long userId, CreateGroupRequest request) {
        User user = users.getReferenceById(userId);
        CarpoolGroup group = groups.save(new CarpoolGroup(request.name().trim(), blankToNull(request.description()),
                uniqueInviteCode(), parseZone(request.timezone()), clock.instant()));
        members.save(new GroupMember(group, user, MemberRole.OWNER, clock.instant()));
        return detail(group.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> listMine(Long userId) {
        return members.findAllForUser(userId).stream()
                .map(m -> new GroupSummary(m.getGroup().getId(), m.getGroup().getName(),
                        m.getGroup().getDescription(), m.getRole(), members.countByGroupId(m.getGroup().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetail detail(Long groupId, Long userId) {
        GroupMember me = access.requireMember(groupId, userId);
        CarpoolGroup group = me.getGroup();
        List<MemberResponse> all = members.findAllInGroup(groupId).stream().map(MemberResponse::from).toList();
        return new GroupDetail(group.getId(), group.getName(), group.getDescription(), group.getZone().getId(),
                group.getInviteCode(), me.getRole(), me.getId(), all);
    }

    @Transactional
    public GroupDetail join(Long userId, String inviteCode) {
        CarpoolGroup group = groups.findByInviteCode(InviteCodes.normalize(inviteCode))
                .orElseThrow(() -> ApiException.notFound("Invite code"));
        if (members.findByGroupIdAndUserId(group.getId(), userId).isEmpty()) {
            members.save(new GroupMember(group, users.getReferenceById(userId), MemberRole.MEMBER, clock.instant()));
        }
        return detail(group.getId(), userId);
    }

    @Transactional
    public GroupDetail update(Long groupId, Long userId, UpdateGroupRequest request) {
        CarpoolGroup group = access.requireManager(groupId, userId).getGroup();
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw ApiException.badRequest("Name cannot be blank");
            }
            group.setName(request.name().trim());
        }
        if (request.description() != null) {
            group.setDescription(blankToNull(request.description()));
        }
        if (request.timezone() != null) {
            group.setZone(parseZone(request.timezone()));
        }
        return detail(groupId, userId);
    }

    @Transactional
    public GroupDetail regenerateInviteCode(Long groupId, Long userId) {
        access.requireManager(groupId, userId).getGroup().setInviteCode(uniqueInviteCode());
        return detail(groupId, userId);
    }

    @Transactional
    public MemberResponse changeRole(Long groupId, Long memberId, Long userId, MemberRole role) {
        access.requireManager(groupId, userId);
        if (role == MemberRole.OWNER) {
            throw ApiException.badRequest("Ownership cannot be assigned");
        }
        GroupMember member = memberInGroup(groupId, memberId);
        if (member.getRole() == MemberRole.OWNER) {
            throw ApiException.badRequest("The owner's role cannot be changed");
        }
        member.setRole(role);
        return MemberResponse.from(member);
    }

    @Transactional
    public void leave(Long groupId, Long userId) {
        GroupMember me = access.requireMember(groupId, userId);
        if (me.getRole() == MemberRole.OWNER && members.countByGroupId(groupId) > 1) {
            throw ApiException.badRequest("Hand the group over to another member before leaving");
        }
        members.delete(me);
        if (me.getRole() == MemberRole.OWNER) {
            groups.delete(me.getGroup());
        }
    }

    @Transactional
    public void removeMember(Long groupId, Long memberId, Long userId) {
        GroupMember admin = access.requireManager(groupId, userId);
        GroupMember member = memberInGroup(groupId, memberId);
        if (member.getId().equals(admin.getId())) {
            throw ApiException.badRequest("Use leave to remove yourself");
        }
        if (member.getRole() == MemberRole.OWNER) {
            throw ApiException.forbidden("The owner cannot be removed");
        }
        members.delete(member);
    }

    @Transactional
    public GroupDetail transferOwnership(Long groupId, Long memberId, Long userId) {
        GroupMember owner = access.requireMember(groupId, userId);
        if (owner.getRole() != MemberRole.OWNER) {
            throw ApiException.forbidden("Only the owner can hand the group over");
        }
        GroupMember next = memberInGroup(groupId, memberId);
        next.setRole(MemberRole.OWNER);
        owner.setRole(MemberRole.ADMIN);
        return detail(groupId, userId);
    }

    @Transactional
    public MemberResponse updateMyMembership(Long groupId, Long userId, UpdateMembershipRequest request) {
        GroupMember me = access.requireMember(groupId, userId);
        if (request.canDrive() && request.carSeats() < 1) {
            throw ApiException.badRequest("Drivers need at least one free seat");
        }
        me.setDriving(request.canDrive(), request.carSeats());
        me.setPickupNote(blankToNull(request.pickupNote()));
        return MemberResponse.from(me);
    }

    GroupMember memberInGroup(Long groupId, Long memberId) {
        return members.findById(memberId)
                .filter(m -> m.getGroup().getId().equals(groupId))
                .orElseThrow(() -> ApiException.notFound("Member"));
    }

    private String uniqueInviteCode() {
        String code;
        do {
            code = InviteCodes.generate();
        } while (groups.existsByInviteCode(code));
        return code;
    }

    static ZoneId parseZone(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(value.trim());
        } catch (DateTimeException ex) {
            throw ApiException.badRequest("Unknown time zone: " + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
