package com.locibot.organizer2.utils;

import com.locibot.organizer2.data.Config;

import java.util.*;

public class I18nManager {

    public static final Locale[] LOCALES = {Locale.ENGLISH, Locale.GERMAN};

    private static final Map<Locale, ResourceBundle> GLOBAL_BUNDLES = I18nManager.initialize("i18n", LOCALES);
    private static final Map<Locale, ResourceBundle> SPAM_BUNDLES = I18nManager.initialize("spam", LOCALES);

    private static Map<Locale, ResourceBundle> initialize(String bundleName, Locale... locales) {
        final Map<Locale, ResourceBundle> map = new HashMap<>(locales.length);
        for (final Locale locale : locales) {
            map.put(locale, ResourceBundle.getBundle("i18n/%s".formatted(bundleName), locale));
        }
        return Collections.unmodifiableMap(map);
    }

    public static ResourceBundle getBundle(final Locale locale) {
        return GLOBAL_BUNDLES.get(locale);
    }

    public static String localize(Locale locale, String key) {
        try {
            return I18nManager.getBundle(locale).getString(key);
        } catch (final MissingResourceException ignored) {
            try {
                return I18nManager.getBundle(Config.DEFAULT_LOCALE).getString(key);
            } catch (final MissingResourceException err) {
                return key;
            }
        }
    }

    public static String localize(Locale locale, double number) {
        return FormatUtil.number(locale, number);
    }

    // TODO Clean-up
    public static String getRandomSpam(Locale locale) {
        final ResourceBundle bundle = SPAM_BUNDLES.get(locale);
        String randomKey = RandUtil.randValue(bundle.keySet());
        if (randomKey == null) {
            randomKey = RandUtil.randValue(SPAM_BUNDLES.get(Config.DEFAULT_LOCALE).keySet());
        }
        assert randomKey != null;
        return bundle.getString(randomKey);
    }

}
