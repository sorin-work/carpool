package com.carpool.group;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class InviteCodesTest {

    @Test
    void generatesCodesFromTheUnambiguousAlphabet() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            String code = InviteCodes.generate(random);
            assertThat(code).hasSize(InviteCodes.LENGTH);
            assertThat(code.chars()).allMatch(c -> InviteCodes.ALPHABET.indexOf(c) >= 0);
        }
    }

    @Test
    void neverUsesLookAlikeCharacters() {
        assertThat(InviteCodes.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    void normalizesWhatPeopleType() {
        assertThat(InviteCodes.normalize(" abcd-efgh ")).isEqualTo("ABCDEFGH");
        assertThat(InviteCodes.normalize("ab cd ef gh")).isEqualTo("ABCDEFGH");
        assertThat(InviteCodes.normalize(null)).isEmpty();
    }
}
