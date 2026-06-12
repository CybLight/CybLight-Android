package org.cyblight.android.data.preferences

enum class AppLockTimeout(val millis: Long) {
    IMMEDIATE(0L),
    SEC_30(30_000L),
    MIN_1(60_000L),
    MIN_5(300_000L),
    MIN_15(900_000L),
    ;

    companion object {
        fun fromMillis(value: Long): AppLockTimeout =
            entries.firstOrNull { it.millis == value } ?: IMMEDIATE
    }
}
