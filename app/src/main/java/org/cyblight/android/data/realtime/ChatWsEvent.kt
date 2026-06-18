package org.cyblight.android.data.realtime

import com.google.gson.annotations.SerializedName

data class ChatWsEvent(
    val type: String = "",
    @SerializedName("messageId") val messageId: String = "",
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("peerId") val peerId: String = "",
    @SerializedName("createdAt") val createdAt: Long = 0L,
)
