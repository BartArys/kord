package com.gitlab.kordlib.gateway.builder

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.common.entity.optional.Optional
import com.gitlab.kordlib.common.entity.optional.OptionalBoolean
import com.gitlab.kordlib.common.entity.optional.OptionalInt
import com.gitlab.kordlib.common.entity.optional.delegate.delegate
import com.gitlab.kordlib.gateway.GuildMembersChunkData
import com.gitlab.kordlib.gateway.Intent
import com.gitlab.kordlib.gateway.PrivilegedIntent
import com.gitlab.kordlib.gateway.RequestGuildMembers

/**
 * A builder for a [RequestGuildMembers] command.
 *
 * @param guildId The id of the guild on which to execute the command.
 */
@OptIn(PrivilegedIntent::class)
class RequestGuildMembersBuilder(var guildId: Snowflake) {

    private var _query: Optional<String> = Optional.Missing()

    /**
     * The prefix to match usernames against. Use an empty string to match against all members.
     * [Intent.GuildMembers] is required when setting the [query] to `""` and [limit] to `0`.
     */
    var query: String? by ::_query.delegate()

    private var _limit: OptionalInt = OptionalInt.Missing

    /**
     * The maximum number of members to match against when using a [query].
     * Use `0` to request all members.
     * [Intent.GuildMembers] is required when setting the [query] to `""` and [limit] to `0`.
     */
    var limit: Int? by ::_limit.delegate()

    private var _presences: OptionalBoolean = OptionalBoolean.Missing

    /**
     * Whether [GuildMembersChunkData.presences] should be present in the response.
     * [Intent.GuildPresences] is required to enable [presences].
     */
    var presences: Boolean? by ::_presences.delegate()

    /**
     * The ids of the user to match against.
     */
    var userIds = mutableSetOf<Snowflake>()

    private var _nonce: Optional<String> = Optional.Missing()

    /**
     * A nonce to identify the [GuildMembersChunkData.nonce] responses.
     */
    var nonce: String? by ::_nonce.delegate()

    /**
     * Utility function that sets the required fields for requesting all members.
     */
    fun requestAllMembers() {
        limit = 0
        query= ""
        userIds.clear()
    }

    fun toRequest(): RequestGuildMembers = RequestGuildMembers(
            guildId, _query, _limit, _presences, Optional.missingOnEmpty(userIds), _nonce
    )

}
