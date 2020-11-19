package com.gitlab.kordlib.common.entity

import com.gitlab.kordlib.common.DiscordBitSet
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class DiscordChannel(
        val id: String,
        val type: ChannelType,
        @SerialName("guild_id")
        val guildId: String? = null,
        val position: Int? = null,
        @SerialName("permission_overwrites")
        val permissionOverwrites: List<Overwrite>? = null,
        val name: String? = null,
        val topic: String? = null,
        val nsfw: Boolean? = null,
        @SerialName("last_message_id")
        val lastMessageId: String? = null,
        val bitrate: Int? = null,
        @SerialName("user_limit")
        val userLimit: Int? = null,
        @SerialName("rate_limit_per_user")
        val rateLimitPerUser: Int? = null,
        val recipients: List<DiscordUser>? = null,
        val icon: String? = null,
        @SerialName("owner_id")
        val ownerId: String? = null,
        @SerialName("application_id")
        val applicationId: String? = null,
        @SerialName("parent_id")
        val parentId: String? = null,
        @SerialName("last_pin_timestamp")
        val lastPinTimestamp: String? = null
)

@Serializable
data class Overwrite(
        val id: String,
        val type: String,
        val allow: DiscordBitSet,
        val deny: DiscordBitSet
)

@Serializable(with = ChannelType.ChannelTypeSerializer::class)
enum class ChannelType(val code: Int) {
    /** The default code for unknown values. */
    Unknown(Int.MIN_VALUE),
    GuildText(0),
    DM(1),
    GuildVoice(2),
    GroupDm(3),
    GuildCategory(4),
    GuildNews(5),
    GuildStore(6);

    companion object ChannelTypeSerializer : KSerializer<ChannelType> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("type", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): ChannelType {
            val code = decoder.decodeInt()
            return values().firstOrNull { it.code == code } ?: Unknown
        }

        override fun serialize(encoder: Encoder, value: ChannelType) {
            encoder.encodeInt(value.code)
        }
    }

}