package com.goggles.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageUtil {

    private static MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        MessageUtil.messageSource = messageSource;
    }

    public static String getMessage(String code) {
        return getMessage(code, null, (Object[]) null);
    }

    public static String getMessage(String code, Object... args) {
        return getMessage(code, null, args);
    }

    public static String getMessage(String code, Locale locale, Object... args) {
        return getMessage(code, null, locale, args);
    }

    public static String getMessage(String code, String defaultMessage, Locale locale, Object... args) {
        Locale currentLocale = locale != null ? locale : LocaleContextHolder.getLocale();

        try {
            return messageSource.getMessage(code, args, defaultMessage, currentLocale);
        } catch (Exception e) {
            return defaultMessage != null ? defaultMessage : code;
        }
    }
}