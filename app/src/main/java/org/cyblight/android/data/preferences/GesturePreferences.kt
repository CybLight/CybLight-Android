package org.cyblight.android.data.preferences

enum class SwipeBackSensitivity(val fraction: Float) {
    LOW(0.35f),
    NORMAL(0.25f),
    HIGH(0.18f),
    ;

    companion object {
        fun fromName(value: String?): SwipeBackSensitivity =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}

enum class SwipeBackEdgeWidth(val widthDp: Int) {
    NARROW(16),
    NORMAL(32),
    WIDE(48),
    ;

    companion object {
        fun fromName(value: String?): SwipeBackEdgeWidth =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}

enum class RootBackBehavior {
    HOME_THEN_EXIT,
    EXIT_IMMEDIATELY,
    MINIMIZE,
    ;

    companion object {
        fun fromName(value: String?): RootBackBehavior =
            entries.firstOrNull { it.name == value } ?: HOME_THEN_EXIT
    }
}
