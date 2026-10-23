package br.com.monitorjudicial.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

    @Test
    void pageQueryClampsValues() {
        assertThat(PageQuery.of(-1, 0)).isEqualTo(new PageQuery(0, 20));
        assertThat(PageQuery.of(2, 1000).size()).isEqualTo(PageQuery.MAX_SIZE);
    }

    @Test
    void pageResultComputesTotalPagesAndMaps() {
        PageResult<Integer> page = PageResult.of(List.of(1, 2), 0, 2, 5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.map(i -> "n" + i).content()).containsExactly("n1", "n2");
        assertThat(PageResult.of(List.of(), 0, 0, 0).totalPages()).isZero();
    }
}
