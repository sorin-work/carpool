package com.carpool.group;

import com.carpool.common.ApiException;
import org.springframework.stereotype.Component;

/**
 * Membership checks shared by every group-scoped endpoint. Non-members get a 404 rather than a 403
 * so group ids cannot be probed.
 */
@Component
public class GroupAccess {

    private final GroupMemberRepository members;

    public GroupAccess(GroupMemberRepository members) {
        this.members = members;
    }

    public GroupMember requireMember(Long groupId, Long userId) {
        return members.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("Group"));
    }

    public GroupMember requireManager(Long groupId, Long userId) {
        GroupMember member = requireMember(groupId, userId);
        if (!member.getRole().canManage()) {
            throw ApiException.forbidden("Only group admins can do this");
        }
        return member;
    }
}
