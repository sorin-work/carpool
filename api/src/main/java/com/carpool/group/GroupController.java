package com.carpool.group;

import com.carpool.group.dto.CreateGroupRequest;
import com.carpool.group.dto.GroupDetail;
import com.carpool.group.dto.GroupSummary;
import com.carpool.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{groupId}")
    public GroupDetail get(@AuthenticationPrincipal AuthUser auth, @PathVariable Long groupId) {
        return groupService.detail(groupId, auth.id());
    }
}
