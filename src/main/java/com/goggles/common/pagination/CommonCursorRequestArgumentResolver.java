package com.goggles.common.pagination;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 파라미터로 선언된 {@link CommonCursorRequest}를 쿼리 파라미터에서 자동 바인딩.
 *
 * <p>{@code ?cursor=xxx&size=10} 형태로 요청하면 {@link CommonCursorRequest}로 변환됩니다.
 * {@code cursor} 파라미터가 없으면 첫 페이지({@code null})로 처리됩니다.
 *
 * <pre>{@code
 * @GetMapping("/feeds")
 * public ApiResponse<CursorResponse<FeedResponse>> list(CommonCursorRequest cursorRequest) {
 *     return ApiResponse.success(feedService.list(cursorRequest));
 * }
 * }</pre>
 */
public class CommonCursorRequestArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CommonCursorRequest.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String cursor = webRequest.getParameter("cursor");
		String sizeParam = webRequest.getParameter("size");

		int size = parseOrDefault(sizeParam);

		return CommonCursorRequest.of(cursor, size);
	}

	private int parseOrDefault(String value) {
		if (value == null) return CommonPageRequest.DEFAULT_SIZE;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return CommonPageRequest.DEFAULT_SIZE;
		}
	}
}