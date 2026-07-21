package io.github.oppsgo.json.kotlin

import io.github.oppsgo.json.annotation.JsonAlias
import io.github.oppsgo.json.annotation.JsonIgnore
import io.github.oppsgo.json.annotation.JsonProperty

data class SimpleUser(
    val id: Int,
    val name: String,
)

data class UserWithDefaults(
    val id: Int,
    val name: String = "anonymous",
    val active: Boolean = true,
)

data class NestedUser(
    val id: Int,
    val profile: SimpleUser,
)

data class RenamedUser(
    @field:JsonProperty("user_name")
    val userName: String,
    val age: Int,
)

data class AliasedUser(
    @field:JsonAlias("nick", "display_name")
    val name: String,
    val id: Int,
)

data class IgnoredFieldUser(
    val id: Int,
    val name: String,
    @field:JsonIgnore
    val secret: String = "hidden",
)

data class NullableUser(
    val id: Int,
    val nickname: String? = null,
)

data class Team(
    val name: String,
    val members: List<SimpleUser>,
)
