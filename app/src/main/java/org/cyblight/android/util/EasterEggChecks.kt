package org.cyblight.android.util

object EasterEggChecks {
    fun hasBold(text: String): Boolean = Regex("""\*\*.+?\*\*""").containsMatchIn(text)

    fun hasItalic(text: String): Boolean = Regex("""(?<![\w\\])_.+?_(?![\w])""").containsMatchIn(text)

    fun hasUnderline(text: String): Boolean = Regex("""__.+?__""").containsMatchIn(text)

    fun hasStrike(text: String): Boolean = Regex("""~~.+?~~""").containsMatchIn(text)

    fun hasMono(text: String): Boolean =
        Regex("""`.+?`""").containsMatchIn(text) || Regex("""```[\s\S]+?```""").containsMatchIn(text)

    fun hasSpoiler(text: String): Boolean = Regex("""\|\|.+?\|\|""").containsMatchIn(text)

    fun hasQuote(text: String): Boolean = Regex("""(?m)^>\s""").containsMatchIn(text)

    fun hasLink(text: String): Boolean = Regex("""\[[^\]]+\]\([^)]+\)""").containsMatchIn(text) || 
        Regex("""https?://[^\s]+""").containsMatchIn(text)

    fun hasCodeBlock(text: String): Boolean = Regex("""```[\s\S]+?```""").containsMatchIn(text)

    fun isTypographerMessage(text: String): Boolean {
        val plain = stripReplyToken(text).trim()
        if (plain.length < 5) return false
        
        val formats = listOf(
            hasBold(plain),
            hasItalic(plain),
            hasUnderline(plain),
            hasStrike(plain),
            hasMono(plain),
            hasSpoiler(plain),
            hasLink(plain),
            hasQuote(plain),
            hasCodeBlock(plain)
        )
        // Award if at least 3 distinct formatting types are used
        return formats.count { it } >= 3
    }

    fun hasFormatting(text: String): Boolean {
        val plain = stripReplyToken(text).trim()
        if (plain.isEmpty()) return false
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

    private val EMOJI_REGEX = Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27ff\uFE0F\u200D]""")

    fun isSilenceMessage(text: String): Boolean {
        val plain = stripReplyToken(text).trim()
        if (plain.isEmpty() || !hasSpoiler(plain)) return false
        
        // Remove all spoilers and see what remains
        val withoutSpoilers = plain.replace(Regex("""(?s)\|\|.+?\|\|"""), "").trim()
        if (withoutSpoilers.isEmpty()) return true
        
        // Check if only emojis, whitespace and punctuation remain
        val clean = withoutSpoilers.replace(EMOJI_REGEX, "")
            .replace(Regex("""[\s\p{Punct}]"""), "")
            .trim()
            
        return clean.isEmpty()
    }

    fun isMidnightNow(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        // From 00:00 to 00:10
        return hour == 0 && minute <= 10
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
