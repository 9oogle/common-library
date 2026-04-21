package com.goggles.common;

import com.goggles.common.pagination.CommonPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void springPage를_CommonPageResponse로_변환한다() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 10), 2);
        CommonPageResponse<String> response = CommonPageResponse.of(page);

        assertThat(response.getContent()).containsExactly("a", "b");
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void mapper로_엔티티를_DTO로_변환한다() {
        var page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(1, 3), 9);
        CommonPageResponse<String> response = CommonPageResponse.of(page, String::valueOf);

        assertThat(response.getContent()).containsExactly("1", "2", "3");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void 직접_생성시_totalPages가_올바르게_계산된다() {
        CommonPageResponse<String> response = CommonPageResponse.of(List.of("a", "b"), 0, 10, 25);

        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }
}