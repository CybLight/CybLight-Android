package org.cyblight.android.ui.main

enum class MainTab(val index: Int) {
    Home(0),
    Friends(1),
    Messages(2),
    Easter(3),
    ;

    companion object {
        fun fromIndex(index: Int): MainTab = when (index) {
            0 -> Home
            1 -> Friends
            2 -> Messages
            3, 4 -> Easter
            else -> Home
        }
    }
}
