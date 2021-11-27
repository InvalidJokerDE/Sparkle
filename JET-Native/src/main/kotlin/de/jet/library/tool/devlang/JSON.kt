package de.jet.library.tool.devlang

import de.jet.library.extension.data.fromJson
import de.jet.library.tool.base.Constructable
import org.intellij.lang.annotations.Language

/**
 * This data class represents a chunk of JSON code stored inside the [value]
 * @author Fruxz
 * @since 1.0
 */
data class JSON(
	@Language("json")
	override val value: String,
) : DevLangObject {

	/**
	 * Converts the [value] to a [T] object using the [fromJson] function
	 */
	inline fun <reified T : Any> fromJson() = value.fromJson<T>()

	companion object : Constructable<JSON> {
		override fun constructor(vararg parameters: Any?): JSON =
			JSON("" + parameters.first())
	}

}