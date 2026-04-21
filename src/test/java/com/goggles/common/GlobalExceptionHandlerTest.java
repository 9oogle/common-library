package com.goggles.common;

import com.goggles.common.exception.GlobalExceptionAdvice;
import com.goggles.common.exception.NotFoundException;
import com.goggles.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class})
@Import(GlobalExceptionAdvice.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void NotFoundException_발생시_404와_에러_포맷으로_응답한다() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다"));
    }

    @Test
    void BadRequestException_발생시_400으로_응답한다() throws Exception {
        mockMvc.perform(get("/test/bad-request").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void 처리되지_않은_예외는_500으로_응답한다() throws Exception {
        mockMvc.perform(get("/test/unknown-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("리소스를 찾을 수 없습니다");
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("잘못된 요청입니다");
        }

        @GetMapping("/test/unknown-error")
        void unknownError() {
            throw new RuntimeException("예상치 못한 에러");
        }
    }
}