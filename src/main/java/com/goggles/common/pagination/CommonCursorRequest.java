package com.goggles.common.pagination;

import lombok.Getter;

/**
 * 커서 기반 페이지 요청 DTO.
 *
 * <p>커서가 {@code null}이면 첫 페이지를 의미합니다.
 * 허용되지 않은 size 값은 {@link CommonPageRequest#DEFAULT_SIZE}(10)으로 대체됩니다.
 *
 * <pre>{@code
 * @GetMapping("/feeds")
 * public ApiResponse<CursorResponse<FeedResponse>> list(CommonCursorRequest cursorRequest) {
 *     return ApiResponse.success(feedService.list(cursorRequest));
 * }
 * }</pre>
 *
 * @see CommonCursorResponse
 */
@Getter
public class CommonCursorRequest {

	private final String cursor; // null이면 첫 페이지
	private final int size;

	private CommonCursorRequest(String cursor, int size) {
		this.cursor = cursor;
		this.size = CommonPageRequest.ALLOWED_SIZES.contains(size) ? size :
				CommonPageRequest.DEFAULT_SIZE;
	}

	public static CommonCursorRequest of(String cursor, int size) {
		return new CommonCursorRequest(cursor, size);
	}

	public boolean isFirst() {
		return cursor == null;
	}
}