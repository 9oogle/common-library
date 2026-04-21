package com.goggles.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC: Mapped Diagnostic Context(로그용 쓰레드 보관함)
 * - 로그가 어떤 요청(Request)에 속해 있는지에 대한 부가 정보를 보관하는 쓰레드별 저장소
 * - 내부적으로 ThreadLocal을 사용. 즉, 요청이 들어온 쓰레드 하나당 하나의 보관함이 배정된다.
 */
public class MdcLoggingFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		// Trace ID 생성
		String traceId = httpRequest.getHeader("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID()
					.toString()
					.replace("-", "")
					.substring(0, 16);
		}
		// URI 및 Method 정보 추출
		String uri = httpRequest.getRequestURI();

		String method = httpRequest.getMethod();

		//  MDC에 주입
		MDC.put("traceId", traceId);
		MDC.put("uri", uri);
		MDC.put("method", method);

		try {
			chain.doFilter(request, response);
		} finally {
			// 요청 종료 시 반드시 클리어
			MDC.clear();
		}
	}
}
