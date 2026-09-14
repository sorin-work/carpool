package com.carpool.user;

import com.carpool.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNamesTest {

    @Test
    void collapsesWhitespace() {
        assertThat(DisplayNames.clean("  Ana \t  Maria  Popescu ")).isEqualTo("Ana Maria Popescu");
    }

    @Test
    void dropsZeroWidthCharacters() {
        assertThat(DisplayNames.clean("Ben\u200b\ufeff")).isEqualTo("Ben");
    }

    @Test
    void composesAccents() {
        assertThat(DisplayNames.clean("Élise")).isEqualTo("Élise");
    }

    @Test
    void keepsEmoji() {
        assertThat(DisplayNames.clean("🚗 Dad")).isEqualTo("🚗 Dad");
        assertThat(DisplayNames.clean("👩\u200d👧 Pool")).isEqualTo("👩\u200d👧 Pool");
    }

    @Test
    void refusesInvisibleNames() {
        assertThatThrownBy(() -> DisplayNames.clean("​  ")).isInstanceOf(ApiException.class);
    }
}
