package com.goggles.common.util;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 현재(부모) 쓰레드의 MDC 컨텍스트 맵을 복사
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 새로운 (자식) 쓰레드에 복사해둔 컨텍스트 주입
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                runnable.run();
            } finally {
                // 자식 쓰레드 작업 종료 후 반드시 비우기 (쓰레드 풀 오염 방지)
                MDC.clear();
            }
        };
    }
}
