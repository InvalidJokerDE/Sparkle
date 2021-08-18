package de.jet.library.tool.mutable

interface Mutable<T> {

	var property: T

	companion object {

		fun <T> default(value: T) = FlexibleMutable(value)

		fun <T> cached(cacheId: String, value: T) = CachedMutable(cacheId, value)

	}

}