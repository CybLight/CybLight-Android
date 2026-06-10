package org.cyblight.android.update

object VersionUtils {
    fun normalize(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = parseParts(normalize(remote))
        val currentParts = parseParts(normalize(current))
        val maxLen = maxOf(remoteParts.size, currentParts.size)

        for (i in 0 until maxLen) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (remotePart > currentPart) return true
            if (remotePart < currentPart) return false
        }
        return false
    }

    private fun parseParts(version: String): List<Int> =
        version.split(".", "-", "_")
            .mapNotNull { part -> part.filter { it.isDigit() }.toIntOrNull() }
            .ifEmpty { listOf(0) }
}
