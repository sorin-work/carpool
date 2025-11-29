package com.carpool.group;

public enum MemberRole {
    /** Created the group; there is always exactly one. */
    OWNER,
    /** Can change group settings, trips and members. */
    ADMIN,
    MEMBER;

    public boolean canManage() {
        return this == OWNER || this == ADMIN;
    }
}
