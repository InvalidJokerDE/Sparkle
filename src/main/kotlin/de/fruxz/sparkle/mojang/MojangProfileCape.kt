package de.fruxz.sparkle.mojang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MojangProfileCape")
data class MojangProfileCape(
    val data: String,
    val url: String
)