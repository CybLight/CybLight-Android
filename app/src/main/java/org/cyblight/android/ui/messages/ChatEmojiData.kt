package org.cyblight.android.ui.messages

data class EmojiCategory(
    val key: String,
    val icon: String,
    val emojis: List<String>,
)

object ChatEmojiData {
    val quickEmojis = listOf("😀", "😍", "😂", "😭", "😎", "🥰", "😮", "👍", "🔥", "🎉", "❤️", "💯")

    val categories = listOf(
        EmojiCategory(
            key = "smileys",
            icon = "🙂",
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😉", "😊", "😇",
                "😋", "😛", "😜", "😎", "🤓", "🧐", "🤔", "🤨", "😐", "😑", "🙄", "😴",
                "😢", "😭", "😡", "🤯", "😱",
            ),
        ),
        EmojiCategory(
            key = "love",
            icon = "😍",
            emojis = listOf("😍", "🥰", "😘", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💕", "💞"),
        ),
        EmojiCategory(
            key = "fire",
            icon = "🔥",
            emojis = listOf("🔥", "✨", "⭐", "🌟", "💫", "💥", "💯", "🎉", "🎊", "🎈", "🎁", "🏆"),
        ),
        EmojiCategory(
            key = "food",
            icon = "🍔",
            emojis = listOf("🍔", "🍕", "🍟", "🌭", "🍣", "🍜", "🍿", "🍪", "🍩", "🍫", "🍓", "☕️", "🧋"),
        ),
        EmojiCategory(
            key = "objects",
            icon = "💡",
            emojis = listOf("💡", "📱", "💻", "⌚️", "🎧", "📷", "🎮", "🔧", "🔒", "🔑", "🧠", "📝"),
        ),
        EmojiCategory(
            key = "flags",
            icon = "🏳️",
            emojis = listOf("🏳️", "🏴", "🏁", "🚩", "🇺🇦", "🇺🇸", "🇬🇧", "🇩🇪", "🇫🇷", "🇮🇹", "🇪🇸", "🇵🇱", "🇯🇵"),
        ),
    )
}
