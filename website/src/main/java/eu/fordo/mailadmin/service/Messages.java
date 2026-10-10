package eu.fordo.mailadmin.service;

import eu.fordo.mailadmin.servlet.LocaleFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resource-bundle-backed i18n lookup. Only `messages_en.properties` ships today, but the
 * bundle is resolved per-request Locale so adding `messages_<lang>.properties` later needs
 * no code change — just drop the file in and wire the language into LocaleFilter's allowlist.
 */
public final class Messages {

    private static final String BUNDLE_BASE = "i18n.messages";

    private final ResourceBundle bundle;

    private Messages(Locale locale) {
        this.bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
    }

    public static Messages of(HttpServletRequest req) {
        Locale locale = (Locale) req.getAttribute(LocaleFilter.ATTR_LOCALE);
        return new Messages(locale != null ? locale : Locale.ENGLISH);
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
