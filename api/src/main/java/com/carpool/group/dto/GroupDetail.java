package com.carpool.group.dto;

import com.carpool.group.MemberRole;

import java.math.BigDecimal;
import java.util.List;

public record GroupDetail(Long id, String name, String description, String timezone, String inviteCode,
                          BigDecimal costPerKm, String currency, MemberRole myRole, Long myMemberId,
                          List<MemberResponse> members) {
}
