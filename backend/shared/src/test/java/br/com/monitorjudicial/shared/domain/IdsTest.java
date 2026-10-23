package br.com.monitorjudicial.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void generatesVersion7RfcVariantIds() {
        UUID id = Ids.newId();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void idsAreUniqueAndTimeOrdered() throws InterruptedException {
        UUID first = Ids.newId();
        Thread.sleep(2);
        UUID second = Ids.newId();
        assertThat(first.getMostSignificantBits() >>> 16).isLessThan(second.getMostSignificantBits() >>> 16);
        var all = new HashSet<UUID>();
        IntStream.range(0, 10_000).forEach(i -> all.add(Ids.newId()));
        assertThat(all).hasSize(10_000);
    }
}
