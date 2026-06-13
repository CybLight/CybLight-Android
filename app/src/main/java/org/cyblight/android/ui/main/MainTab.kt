package org.cyblight.android.ui.main

enum class MainTab(val index: Int) {
    Home(0),
    Friends(1),
    Messages(2),
    Security(3),
    Easter(4),
    ;

    companion object {
        fun fromIndex(index: Int): MainTab =
            entries.firstOrNull { it.index == index } ?: Home
    }
}
