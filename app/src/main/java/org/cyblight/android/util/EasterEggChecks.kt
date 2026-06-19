package org.cyblight.android.util

object EasterEggChecks {
    fun hasBold(text: String): Boolean = Regex("""\*\*.+?\*\""").containsMatchIn(text)

    fun hasItalic(text: String): Boolean = Regex("""(?<![\w])_.+?_(?![\w])""").containsMatchIn(text)

    fun hasUnderline(text: String): Boolean = Regex("""__.+?__""").containsMatchIn(text)

    fun hasStrike(text: String): Boolean = Regex("""~~.+?~~""").containsMatchIn(text)

    fun hasMono(text: String): Boolean =
        Regex("""`.+?`""").containsMatchIn(text) || Regex("""```[\s\S]+?```""").containsMatchIn(text)

    fun hasSpoiler(text: String): Boolean = Regex("""\|\|.+?\|\|""").containsMatchIn(text)

    fun hasQuote(text: String): Boolean = Regex("""(?m)^>\s""").containsMatchIn(text)

    fun hasLink(text: String): Boolean = Regex("""\[[^\]]+\]\([^)]+\)""").containsMatchIn(text)

    fun hasCodeBlock(text: String): Boolean = Regex("""```[\s\S]+?```""").containsMatchIn(text)

    fun isTypographerMessage(text: String): Boolean {
        val plain = stripReplyToken(text)
        return hasBold(plain) &&
            hasItalic(plain) &&
            hasUnderline(plain) &&
            hasStrike(plain) &&
            hasMono(plain) &&
            hasSpoiler(plain) &&
            hasQuote(plain) &&
            hasLink(plain) &&
            hasCodeBlock(plain)
    }

    fun hasFormatting(text: String): Boolean {
        val plain = stripReplyToken(text)
        return hasBold(plain) ||
            hasItalic(plain) ||
            hasUnderline(plain) ||
            hasStrike(plain) ||
            hasMono(plain) ||
            hasSpoiler(plain) ||
            hasQuote(plain) ||
            hasLink(plain) ||
            hasCodeBlock(plain)
    }

    fun isSilenceMessage(text: String): Boolean {
        val plain = stripReplyToken(text).trim()
        if (plain.isEmpty() || !hasSpoiler(plain)) return false
        val withoutSpoilers = plain.replace(Regex("""\|\|.+?\|\|"""), "")
        return withoutSpoilers.isNotEmpty() && withoutSpoilers.all { it.isWhitespace() || isEmojiChar(it) }
    }

    fun isMidnightNow(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return hour == 0 && minute <= 5
    }

    private fun stripReplyToken(text: String): String =
        text.replace(Regex("""\[\[CYBLIGHT_REPLY:[^\]]+\]\]\s*"""), "")

    private fun isEmojiChar(char: Char): Boolean {
        val type = Character.getType(char)
        return type == Character.SURROGATE.toInt() ||
            type == Character.OTHER_SYMBOL.toInt() ||
            type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt()
    }
}
