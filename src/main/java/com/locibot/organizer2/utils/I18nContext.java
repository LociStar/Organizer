package com.locibot.organizer2.utils;

import java.util.Locale;

public interface I18nContext {

    Locale getLocale();

    String localize(String key);

    String localize(double number);

}
