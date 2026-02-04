package com.carpool.cost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns balances into a short list of payments that settles everyone up: repeatedly the member who
 * owes the most pays the member who is owed the most.
 */
public final class SettlementPlanner {

    public record Transfer(Long fromMemberId, String fromName, Long toMemberId, String toName, long amountCents) {
    }

    private SettlementPlanner() {
    }

    public static List<Transfer> settle(List<MemberBalance> balances) {
        List<long[]> debtors = new ArrayList<>();
        List<long[]> creditors = new ArrayList<>();
        Map<Long, String> names = new HashMap<>();
        for (MemberBalance b : balances) {
            names.put(b.memberId(), b.displayName());
            if (b.balanceCents() < 0) {
                debtors.add(new long[]{b.memberId(), -b.balanceCents()});
            } else if (b.balanceCents() > 0) {
                creditors.add(new long[]{b.memberId(), b.balanceCents()});
            }
        }
        Comparator<long[]> largestFirst = Comparator.comparingLong((long[] e) -> -e[1]).thenComparingLong(e -> e[0]);
        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            debtors.sort(largestFirst);
            creditors.sort(largestFirst);
            long[] debtor = debtors.get(0);
            long[] creditor = creditors.get(0);
            long amount = Math.min(debtor[1], creditor[1]);
            transfers.add(new Transfer(debtor[0], names.get(debtor[0]), creditor[0], names.get(creditor[0]), amount));
            debtor[1] -= amount;
            creditor[1] -= amount;
            if (debtor[1] == 0) {
                debtors.remove(0);
            }
            if (creditor[1] == 0) {
                creditors.remove(0);
            }
        }
        return transfers;
    }
}
