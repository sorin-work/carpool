package com.carpool.cost;

/**
 * Money owed to ({@code balanceCents > 0}) or by a member over a period. {@code earnedCents} is what
 * riders owe them for driving, {@code spentCents} what they owe for riding.
 */
public record MemberBalance(Long memberId, String displayName, long earnedCents, long spentCents, long balanceCents) {
}
