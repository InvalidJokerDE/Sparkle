package de.fruxz.sparkle.mojang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MojangProfileTextures")
data class MojangProfileTextures constructor(
	val cape: MojangProfileCape = MojangProfileCape("empty", "empty"),
	val custom: Boolean,
	val raw: MojangProfileRaw,
	val skin: MojangProfileSkin,
	val slim: Boolean
)