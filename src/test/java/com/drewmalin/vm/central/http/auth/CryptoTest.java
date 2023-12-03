package com.drewmalin.vm.central.http.auth;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CryptoTest {

    @Test
    public void shouldAnswerWithTrue() {
        final var salt = new byte[]{109, -43, 36, -105, 75, -1, -10, -21, -19, -84, 17, -80, 52, 96, -70, -92};
        final var pass = Crypto.builder("   password the is admin this").salt(salt).build();

        final var passBytes = new byte[]{-44, 44, 23, 12, 111, -39, -26, 124, 125, -19, 12, -103, 35, 126, 29, 51};

        assertThat(pass.hash(), is(passBytes));
    }
}


