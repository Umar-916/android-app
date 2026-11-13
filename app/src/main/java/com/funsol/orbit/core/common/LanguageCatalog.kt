package com.funsol.orbit.core.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class AppLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false,
) {
    val label: String get() = if (englishName == nativeName) nativeName else "$englishName ($nativeName)"
}

object LanguageCatalog {
    val languages = listOf(
        AppLanguage("en", "English", "English", "🇺🇸"),
        AppLanguage("ur", "Urdu", "اردو", "🇵🇰", isRtl = true),
        AppLanguage("ar", "Arabic", "العربية", "🇸🇦", isRtl = true),
        AppLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
        AppLanguage("bn", "Bangla", "বাংলা", "🇧🇩"),
        AppLanguage("es", "Spanish", "Español", "🇪🇸"),
        AppLanguage("fr", "French", "Français", "🇫🇷"),
        AppLanguage("de", "German", "Deutsch", "🇩🇪"),
        AppLanguage("pt-BR", "Portuguese", "Português (Brasil)", "🇧🇷"),
        AppLanguage("ru", "Russian", "Русский", "🇷🇺"),
        AppLanguage("tr", "Turkish", "Türkçe", "🇹🇷"),
        AppLanguage("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
    )

    const val DEFAULT_CODE = "en"

    fun byCode(code: String): AppLanguage = languages.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: languages.first()

    fun apply(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(byCode(code).code))
    }

    fun suggested(locale: Locale = Locale.getDefault()): AppLanguage =
        languages.firstOrNull { it.code.equals(locale.toLanguageTag(), true) }
            ?: languages.firstOrNull { it.code.substringBefore('-').equals(locale.language, true) }
            ?: languages.first()
}
