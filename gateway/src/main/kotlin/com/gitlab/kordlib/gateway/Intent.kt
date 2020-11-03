package com.gitlab.kordlib.gateway

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.RequiresOptIn.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Some intents are defined as "Privileged" due to the sensitive nature of the data and cannot be used by Kord without enabling them.
 *
 * See [the official documentation](https://discord.com/developers/docs/topics/gateway#privileged-intents) for more info on
 * how to enable these.
 */
@RequiresOptIn("""
    Some intents are defined as "Privileged" due to the sensitive nature of the data and cannot be used by Kord without enabling them.
    
    See https://discord.com/developers/docs/topics/gateway#privileged-intents for more info on how to enable these.
""", Level.ERROR)
annotation class PrivilegedIntent

/**
 * Values that enable a group of events as [defined by Discord](https://github.com/discord/discord-api-docs/blob/feature/gateway-intents/docs/topics/Gateway.md#gateway-intents).
 */
enum class Intent(val code: Int) {
    /**
     * Enables the following events:
     * - [GuildCreate]
     * - [GuildDelete]
     * - [GuildRoleCreate]
     * - [GuildRoleUpdate]
     * - [GuildRoleDelete]
     * - [ChannelCreate]
     * - [ChannelUpdate]
     * - [ChannelDelete]
     * - [ChannelPinsUpdate]
     */
    Guilds(1 shl 0),

    /**
     * Enables the following events:
     * - [GuildMemberAdd]
     * - [GuildMemberUpdate]
     * - [GuildMemberRemove]
     */
    @PrivilegedIntent
    GuildMembers(1 shl 1),

    /**
     * Enables the following events:
     * - [GuildBanAdd]
     * - [GuildBanRemove]
     */
    GuildBans(1 shl 2),

    /**
     * Enables the following events:
     * - [GuildEmojisUpdate]
     */
    GuildEmojis(1 shl 3),

    /**
     * Enables the following events:
     * - [GuildIntegrationsUpdate]
     */
    GuildIntegrations(1 shl 4),

    /**
     * Enables the following events:
     * - [WebhooksUpdate]
     */
    GuildWebhooks(1 shl 5),

    /**
     * Enables the following events:
     * - INVITE_CREATE
     * - INVITE_DELETE
     */
    GuildInvites(1 shl 6),

    /**
     * Enables the following events:
     * - [VoiceStateUpdate]
     */
    GuildVoiceStates(1 shl 7),

    /**
     * Enables the following events:
     * - [PresenceUpdate]
     */
    @PrivilegedIntent
    GuildPresences(1 shl 8),

    /**
     * Enables the following events:
     * - [MessageCreate]
     * - [MessageUpdate]
     * - [MessageDelete]
     * - [MessageDeleteBulk]
     */
    GuildMessages(1 shl 9),

    /**
     * Enables the following events:
     * - [MessageReactionAdd]
     * - [MessageReactionRemove]
     * - [MessageReactionRemoveAll]
     * - MESSAGE_REACTION_REMOVE_EMOJI
     */
    GuildMessageReactions(1 shl 10),

    /**
     * Enables the following events:
     * - [TypingStart]
     */
    GuildMessageTyping(1 shl 11),

    /**
     * Enables the following events:
     * - [ChannelCreate]
     * - [ChannelDelete]
     * - [MessageUpdate]
     * - [MessageDelete]
     */
    DirectMessages(1 shl 12),

    /**
     * Enables the following events:
     * - [MessageReactionAdd]
     * - [MessageReactionRemove]
     * - [MessageReactionRemoveAll]
     * - MESSAGE_REACTION_REMOVE_EMOJI
     */
    DirectMessagesReactions(1 shl 13),

    /**
     * Enables the following events:
     * - [TypingStart]
     */
    DirectMessageTyping(1 shl 14)
}

/**
 * A set of [intents][Intent] to be used while [identifying][Identify] a [Gateway] connection to communicate the events the client wishes to receive.
 */
@Serializable(with = IntentsSerializer::class)
data class Intents internal constructor(val code: Int) {

    val intents = Intent.values().filter { code and it.code != 0 }.toSet()

    operator fun contains(intent: Intent) = intent in intents

    /**
     * Returns an [Intents] that added the [intent] to this [code].
     */
    operator fun plus(intent: Intent): Intents = Intents(code or intent.code)

    /**
     * Returns an [Intents] that removed the [intent] from this [code].
     */
    operator fun minus(intent: Intent): Intents = Intents(intent.code xor (code and intent.code))

    /**
     * copy this [Intents] and apply the [block] to it.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun copy(block: IntentsBuilder.() -> Unit): Intents {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val builder = IntentsBuilder(code)
        builder.apply(block)
        return builder.flags()
    }

    companion object {

        @PrivilegedIntent
        val all: Intents
            get() = invoke {
                Intent.values().forEach { +it }
            }

        @OptIn(PrivilegedIntent::class)
        val nonPrivileged: Intents
            get() = invoke {
                +all
                -Intent.GuildPresences
                -Intent.GuildMembers
            }

        val none: Intents = invoke()

        inline operator fun invoke(builder: IntentsBuilder.() -> Unit = {}): Intents {
            return IntentsBuilder().apply(builder).flags()
        }

        operator fun invoke(vararg intents: Intents) = invoke {
            intents.forEach { +it }
        }

        operator fun invoke(vararg intents: Intent) = invoke {
            intents.forEach { +it }
        }

        @JvmName("invokeWithIntents")
        operator fun invoke(intents: Iterable<Intents>) = invoke {
            intents.forEach { +it }
        }


        operator fun invoke(intents: Iterable<Intent>) = invoke {
            intents.forEach { +it }
        }
    }

    class IntentsBuilder(internal var code: Int = 0) {
        operator fun Intents.unaryPlus() {
            this@IntentsBuilder.code = this@IntentsBuilder.code or code
        }

        operator fun Intent.unaryPlus() {
            this@IntentsBuilder.code = this@IntentsBuilder.code or code
        }

        operator fun Intent.unaryMinus() {
            if (this@IntentsBuilder.code and code == code) {
                this@IntentsBuilder.code = this@IntentsBuilder.code xor code
            }
        }

        fun flags() = Intents(code)
    }

}

object IntentsSerializer : KSerializer<Intents> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("intents", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Intents {
        val flags = decoder.decodeInt()
        return Intents(flags)
    }

    override fun serialize(encoder: Encoder, value: Intents) {
        encoder.encodeInt(value.code)
    }
}
