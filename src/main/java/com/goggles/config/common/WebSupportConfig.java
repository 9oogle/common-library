package com.goggles.config.common;

import com.goggles.common.filter.MdcLoggingFilter;
import com.goggles.common.pagination.CommonCursorRequestArgumentResolver;
import com.goggles.common.pagination.CommonPageRequestArgumentResolver;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class WebSupportConfig {

  @Bean
  public MdcLoggingFilter mdcLoggingFilter() {
    return new MdcLoggingFilter();
  }

  @Bean
  public WebMvcConfigurer commonArgumentResolverConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CommonPageRequestArgumentResolver());
        resolvers.add(new CommonCursorRequestArgumentResolver());
      }
    };
  }
}