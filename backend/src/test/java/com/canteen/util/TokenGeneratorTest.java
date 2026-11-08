package com.canteen.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    @Test
    @DisplayName("Tokens run A001..A100, then B001")
    void format() {
        assertThat(TokenGenerator.format(1)).isEqualTo("A001");
        assertThat(TokenGenerator.format(100)).isEqualTo("A100");
        assertThat(TokenGenerator.format(101)).isEqualTo("B001");
        assertThat(TokenGenerator.format(201)).isEqualTo("C001");
    }
}
