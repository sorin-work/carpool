package com.carpool.group;

import com.carpool.group.dto.ChangeRoleRequest;
import com.carpool.group.dto.CreateGroupRequest;
import com.carpool.group.dto.GroupDetail;
import com.carpool.group.dto.GroupSummary;
import com.carpool.group.dto.InvitePreview;
import com.carpool.group.dto.JoinGroupRequest;
import com.carpool.group.dto.MemberResponse;
import com.carpool.group.dto.UpdateGroupRequest;
import com.carpool.group.dto.UpdateMembershipRequest;
import com.carpool.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupSummary> list(@AuthenticationPrincipal AuthUser auth) {
        return groupService.listMine(auth.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetail create(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(auth.id(), request);
    }

    @PostMapping("/join")
    public GroupDetail join(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody JoinGroupRequest request) {
        return groupService.join(auth.id(), request.inviteCode());
    }

    @GetMapping("/invite/{code}")
    public InvitePreview preview(@AuthenticationPrincipal AuthUser auth, @PathVariable String code) {
        return groupService.preview(auth.id(), code);
    }

    @GetMapping("/{groupId}")
    public GroupDetail get(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return groupService.detail(groupId, auth.id());
    }

    @PatchMapping("/{groupId}")
    public GroupDetail update(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                              @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.update(groupId, auth.id(), request);
    }

    @PostMapping("/{groupId}/invite-code")
    public GroupDetail regenerateInviteCode(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return groupService.regenerateInviteCode(groupId, auth.id());
    }

    @PutMapping("/{groupId}/members/me")
    public MemberResponse updateMyMembership(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                             @Valid @RequestBody UpdateMembershipRequest request) {
        return groupService.updateMyMembership(groupId, auth.id(), request);
    }

    @PutMapping("/{groupId}/members/{memberId}/role")
    public MemberResponse changeRole(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                     @PathVariable Long memberId, @Valid @RequestBody ChangeRoleRequest request) {
        return groupService.changeRole(groupId, memberId, auth.id(), request.role());
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        groupService.delete(groupId, auth.id());
    }

    @PostMapping("/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        groupService.leave(groupId, auth.id());
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                             @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId, auth.id());
    }

    @PostMapping("/{groupId}/members/{memberId}/ownership")
    public GroupDetail transferOwnership(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId,
                                         @PathVariable Long memberId) {
        return groupService.transferOwnership(groupId, memberId, auth.id());
    }
}
