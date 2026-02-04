package com.carpool.cost;

import com.carpool.cost.SettlementPlanner.Transfer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementPlannerTest {

    private static MemberBalance balance(long id, long cents) {
        return new MemberBalance(id, "M" + id, Math.max(cents, 0), Math.max(-cents, 0), cents);
    }

    @Test
    void oneDebtorOneCreditor() {
        assertThat(SettlementPlanner.settle(List.of(balance(1, 500), balance(2, -500))))
                .containsExactly(new Transfer(2L, "M2", 1L, "M1", 500));
    }

    @Test
    void largestDebtPaysLargestCreditFirst() {
        List<Transfer> transfers = SettlementPlanner.settle(List.of(
                balance(1, 900), balance(2, 300), balance(3, -1000), balance(4, -200)));

        assertThat(transfers).containsExactly(
                new Transfer(3L, "M3", 1L, "M1", 900),
                new Transfer(4L, "M4", 2L, "M2", 200),
                new Transfer(3L, "M3", 2L, "M2", 100));
    }

    @Test
    void settledGroupsNeedNoTransfers() {
        assertThat(SettlementPlanner.settle(List.of(balance(1, 0), balance(2, 0)))).isEmpty();
    }

    @Test
    void transfersAddUpToTheDebts() {
        List<MemberBalance> balances = List.of(balance(1, 333), balance(2, 667), balance(3, -400), balance(4, -600));

        long paid = SettlementPlanner.settle(balances).stream().mapToLong(Transfer::amountCents).sum();

        assertThat(paid).isEqualTo(1000);
    }
}
