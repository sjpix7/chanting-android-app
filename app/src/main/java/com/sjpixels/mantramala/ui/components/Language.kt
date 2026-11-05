package com.sjpixels.mantramala.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguage(val tag: String, val flag: String, val nativeName: String)

object AppLanguages {
    val all = listOf(
        AppLanguage("en", "🇬🇧", "English"),
        AppLanguage("hi", "🇮🇳", "हिन्दी"),
        AppLanguage("ru", "🇷🇺", "Русский"),
        AppLanguage("es", "🇪🇸", "Español"),
        AppLanguage("de", "🇩🇪", "Deutsch"),
        AppLanguage("fr", "🇫🇷", "Français"),
        AppLanguage("zh-CN", "🇨🇳", "中文"),
        AppLanguage("ja", "🇯🇵", "日本語"),
        AppLanguage("ko", "🇰🇷", "한국어"),
    )

    /** The app's language: the user's in-app choice if any, else the phone's language if supported, else English. */
    fun current(): AppLanguage {
        val chosen = AppCompatDelegate.getApplicationLocales()
        val locale = (if (!chosen.isEmpty) chosen[0] else LocaleListCompat.getAdjustedDefault()[0])
            ?: return all.first()
        return all.firstOrNull { it.tag.substringBefore('-') == locale.language } ?: all.first()
    }

    /** AppCompat persists this choice itself (autoStoreLocales) and integrates with Android 13+ per-app language. */
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
