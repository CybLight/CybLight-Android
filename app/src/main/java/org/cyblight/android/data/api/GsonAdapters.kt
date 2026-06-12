package org.cyblight.android.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

fun createApiGson(): Gson = GsonBuilder()
    .registerTypeAdapter(MessageDto::class.java, MessageDtoDeserializer())
    .registerTypeAdapter(UserDto::class.java, UserDtoDeserializer())
    .create()

private class UserDtoDeserializer : JsonDeserializer<UserDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): UserDto {
        if (!json.isJsonObject) {
            return UserDto(id = "", login = "")
        }
        val obj = json.asJsonObject
        return UserDto(
            id = obj.readString("id"),
            login = obj.readString("login"),
            publicId = obj.readNullableLong("publicId", "public_id"),
            role = obj.readString("role").ifBlank { null },
        )
    }
}

private class MessageDtoDeserializer : JsonDeserializer<MessageDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): MessageDto {
        if (!json.isJsonObject) {
            return MessageDto()
        }
        val obj = json.asJsonObject
        return MessageDto(
            id = obj.readString("id"),
            senderId = obj.readString("senderId", "sender_id"),
            content = obj.readString("content"),
            createdAt = obj.readLong("createdAt", "created_at"),
            readAt = obj.readNullableLong("readAt", "read_at"),
            editedAt = obj.readNullableLong("editedAt", "edited_at"),
        )
    }
}

private fun JsonObject.readString(vararg keys: String): String {
    for (key in keys) {
        val element = get(key) ?: continue
        if (element.isJsonNull) continue
        if (element.isJsonPrimitive) {
            val primitive = element.asJsonPrimitive
            return when {
                primitive.isString -> primitive.asString
                primitive.isNumber -> primitive.asNumber.toString()
                primitive.isBoolean -> primitive.asBoolean.toString()
                else -> continue
            }
        }
    }
    return ""
}

private fun JsonObject.readLong(vararg keys: String): Long {
    for (key in keys) {
        val element = get(key) ?: continue
        if (element.isJsonNull) continue
        if (element.isJsonPrimitive) {
            val primitive = element.asJsonPrimitive
            val parsed = when {
                primitive.isNumber -> primitive.asLong
                primitive.isString -> primitive.asString.trim().toLongOrNull()
                else -> null
            }
            if (parsed != null) return parsed
        }
    }
    return 0L
}

private fun JsonObject.readNullableLong(vararg keys: String): Long? {
    for (key in keys) {
        val element = get(key) ?: continue
        if (element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val primitive = element.asJsonPrimitive
            val parsed = when {
                primitive.isNumber -> primitive.asLong
                primitive.isString -> primitive.asString.trim().toLongOrNull()
                else -> null
            }
            if (parsed != null) return parsed
        }
    }
    return null
}
