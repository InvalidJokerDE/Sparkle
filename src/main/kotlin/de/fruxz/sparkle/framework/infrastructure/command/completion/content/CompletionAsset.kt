package de.fruxz.sparkle.framework.infrastructure.command.completion.content

import de.fruxz.ascend.extension.container.firstOrNull
import de.fruxz.ascend.extension.container.mapToString
import de.fruxz.ascend.extension.container.withMap
import de.fruxz.ascend.extension.math.isDouble
import de.fruxz.ascend.extension.math.isLong
import de.fruxz.ascend.extension.tryOrNull
import de.fruxz.sparkle.framework.data.Preference
import de.fruxz.sparkle.framework.effect.sound.SoundLibrary
import de.fruxz.sparkle.framework.extension.coroutines.key
import de.fruxz.sparkle.framework.extension.interchange.InterchangeExecutor
import de.fruxz.sparkle.framework.extension.offlinePlayer
import de.fruxz.sparkle.framework.extension.offlinePlayers
import de.fruxz.sparkle.framework.extension.onlinePlayers
import de.fruxz.sparkle.framework.extension.playerOrNull
import de.fruxz.sparkle.framework.extension.plugins
import de.fruxz.sparkle.framework.extension.sparkle
import de.fruxz.sparkle.framework.extension.worlds
import de.fruxz.sparkle.framework.identification.KeyedIdentifiable
import de.fruxz.sparkle.framework.infrastructure.app.App
import de.fruxz.sparkle.framework.infrastructure.app.cache.CacheDepthLevel
import de.fruxz.sparkle.framework.infrastructure.command.Interchange
import de.fruxz.sparkle.framework.infrastructure.command.completion.InterchangeStructureInputRestriction
import de.fruxz.sparkle.framework.infrastructure.component.Component
import de.fruxz.sparkle.framework.infrastructure.service.Service
import de.fruxz.sparkle.framework.sandbox.SandBox
import de.fruxz.sparkle.framework.visual.color.ColorType
import de.fruxz.sparkle.framework.visual.color.DyeableMaterial
import de.fruxz.sparkle.framework.visual.message.Transmission
import de.fruxz.sparkle.server.SparkleCache
import de.fruxz.stacked.extension.subKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.structure.Structure
import java.nio.file.Path
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrSelf
import kotlin.io.path.walk

data class CompletionAsset<T>(
	override val identityKey: Key,
	val refreshing: Boolean,
	val supportedInputType: List<InterchangeStructureInputRestriction<*>> = listOf(InterchangeStructureInputRestriction.STRING),
	val check: (CompletionContext.() -> Boolean)? = null,
	val transformer: (CompletionContext.() -> T?)? = null,
	val generator: CompletionContext.(CompletionAsset<T>) -> Collection<String>,
) : KeyedIdentifiable<CompletionAsset<T>> {

	data class CompletionContext(
		val executor: InterchangeExecutor,
		val fullLineInput: List<String>,
		val input: String,
		val ignoreCase: Boolean,
	)

	fun computedContent(context: CompletionContext): Set<String> = if (!refreshing && SparkleCache.registeredCompletionAssetStateCache.containsKey(identity)) {
		SparkleCache.registeredCompletionAssetStateCache[identity]!!
	} else {
		generator(context, this).toSortedSet().apply {
			if (!refreshing) SparkleCache.registeredCompletionAssetStateCache += identity to this
		}
	}

	fun doCheck(check: (CompletionContext.() -> Boolean)?) = copy(check = check)

	fun transformer(transformer: (CompletionContext.() -> T?)?) = copy(transformer = transformer)

	companion object {

		@JvmStatic
		val LONG = CompletionAsset<Long>(sparkle.subKey("long"), false, listOf(InterchangeStructureInputRestriction.LONG)) {
			(0..99).mapToString()
		}.doCheck {
			input.isLong()
		}.transformer {
			input.toLong()
		}

		@JvmStatic
		val DOUBLE = CompletionAsset<Double>(sparkle.subKey("double"), false, listOf(InterchangeStructureInputRestriction.DOUBLE)) {
			setOf(.0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1.0).mapToString()
		}.doCheck {
			input.isDouble()
		}.transformer {
			input.toDouble()
		}

        @JvmStatic
        val NUMBER = CompletionAsset<Number>(sparkle.subKey("number"), false, listOf(InterchangeStructureInputRestriction.NUMBER)) {
			setOf(0, .5, 1, 1.5, 2).mapToString()
        }.doCheck {
			input.isLong() || input.isDouble()
        }.transformer {
			input.toLongOrNull() ?: input.toDoubleOrNull()
        }

		@JvmStatic
		val ONLINE_PLAYER_NAME = CompletionAsset<Player>(sparkle.subKey("online_player_name"), true, listOf(
			InterchangeStructureInputRestriction.ONLINE_PLAYER)) {
			onlinePlayers.withMap { name }
		}.doCheck {
			playerOrNull(input) != null
		}.transformer {
			playerOrNull(input)
		}

		@JvmStatic
		val ONLINE_PLAYER_UUID = CompletionAsset<Player>(sparkle.subKey("online_player_uuid"), true, listOf(
			InterchangeStructureInputRestriction.ONLINE_PLAYER)) {
			onlinePlayers.withMap { "$uniqueId" }
		}.doCheck {
			tryOrNull { UUID.fromString(input) }?.let { uuid -> return@let playerOrNull(uuid) } != null
		}.transformer {
			tryOrNull { playerOrNull(UUID.fromString(input)) }
		}

		@JvmStatic
		val OFFLINE_PLAYER_NAME = CompletionAsset<OfflinePlayer>(sparkle.subKey("offline_player_name"), true, listOf(
			InterchangeStructureInputRestriction.OFFLINE_PLAYER)) {
			offlinePlayers.withMap { name }.filterNotNull()
		}.doCheck {
			offlinePlayer(input).name != null
		}.transformer {
			offlinePlayer(input)
		}

		@JvmStatic
		val OFFLINE_PLAYER_UUID = CompletionAsset<OfflinePlayer>(sparkle.subKey("offline_player_uuid"), true, listOf(
			InterchangeStructureInputRestriction.OFFLINE_PLAYER)) {
			offlinePlayers.withMap { "$uniqueId" }
		}.doCheck {
			tryOrNull { offlinePlayer(UUID.fromString(input)).name } != null
		}.transformer {
			tryOrNull { offlinePlayer(UUID.fromString(input)) }
		}

		@JvmStatic
		val GAME_MODE = CompletionAsset<GameMode>(sparkle.subKey("gamemode"), false) {
			GameMode.values().withMap { name }
		}.doCheck {
			GameMode.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			GameMode.values().firstOrNull { it.name.equals(input, ignoreCase) }
		}

		@JvmStatic
		val ENTITY_TYPE = CompletionAsset<EntityType>(sparkle.subKey("entity_type"), false) {
			EntityType.values().withMap { name }
		}.doCheck {
			EntityType.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			EntityType.valueOf(input)
		}

		@JvmStatic
		val WORLD_NAME = CompletionAsset<World>(sparkle.subKey("world_name"), true) {
			worlds.withMap { name }
		}.doCheck {
			worlds.any { it.name.equals(input, ignoreCase) }
		}.transformer {
			Bukkit.getWorld(input)
		}

		@JvmStatic
		val APP = CompletionAsset<App>(sparkle.subKey("app"), true) {
			SparkleCache.registeredApps.withMap { identity }
		}.doCheck {
			SparkleCache.registeredApps.any { it.identity.equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.registeredApps.firstOrNull { it.identity == input }
		}

		@JvmStatic
		val PLUGIN = CompletionAsset<Plugin>(sparkle.subKey("plugin"), true) {
			plugins.withMap { key.asString() }
		}.doCheck {
			plugins.any { it.name.lowercase().equals(input.split(":").lastOrNull(), ignoreCase) }
		}.transformer {
			plugins.firstOrNull { it.name.lowercase().equals(input.split(":").lastOrNull(), ignoreCase) }
		}

		@JvmStatic
		val INTERCHANGE = CompletionAsset<Interchange>(sparkle.subKey("interchange"), true) {
			SparkleCache.registeredInterchanges.withMap { identity }
		}.doCheck {
			SparkleCache.registeredInterchanges.any { it.identity.equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.registeredInterchanges.firstOrNull { it.identity == input }
		}

		@JvmStatic
		val SERVICE = CompletionAsset<Service>(sparkle.subKey("service"), true) {
			SparkleCache.services.map { it.key.asString() }
		}.doCheck {
			SparkleCache.services.any { it.key.asString().equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.serviceStates.firstOrNull { it.key.asString() == input }?.value?.service
		}

		@JvmStatic
		val COMPONENT = CompletionAsset<Component>(sparkle.subKey("component"), true, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			SparkleCache.registeredComponents.withMap { identity }
		}.doCheck {
			SparkleCache.registeredComponents.any { it.identity.equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.registeredComponents.firstOrNull { it.identity == input }
		}

		@JvmStatic
		val SANDBOX = CompletionAsset<SandBox>(sparkle.subKey("sandbox"), true, listOf(InterchangeStructureInputRestriction.STRING)) {
			SparkleCache.registeredSandBoxes.withMap { identity }
		}.doCheck {
			SparkleCache.registeredSandBoxes.any { it.identity.equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.registeredSandBoxes.firstOrNull { it.identity == input }
		}

		@JvmStatic
		val PREFERENCE = CompletionAsset<Preference<*>>(sparkle.subKey("preference"), true, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			SparkleCache.registeredPreferences.keys.withMap { identity }
		}.doCheck {
			SparkleCache.registeredPreferences.keys.any { it.identity.equals(input, ignoreCase) }
		}.transformer {
			SparkleCache.registeredPreferences.toList().firstOrNull { it.first.identity == input }?.second
		}

		@JvmStatic
		val CACHE_DEPTH_LEVEL = CompletionAsset<CacheDepthLevel>(sparkle.subKey("cache_depth"), false, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			CacheDepthLevel.values().withMap { name }
		}.doCheck {
			CacheDepthLevel.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			tryOrNull { CacheDepthLevel.valueOf(input) }
		}

		@JvmStatic
		val TRANSMISSION_LEVEL = CompletionAsset<Transmission.Level>(sparkle.subKey("transmission_level"), false, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			Transmission.Level.values().withMap { name }
		}.doCheck {
			Transmission.Level.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			Transmission.Level.values().firstOrNull { it.name.equals(input, true) }
		}

		@JvmStatic
		val LIBRARY_SOUND = CompletionAsset<SoundLibrary>(sparkle.subKey("library_sound"), false, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			SoundLibrary.values().withMap { name }
		}.doCheck {
			SoundLibrary.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			SoundLibrary.values().firstOrNull { it.name == input }
		}

		@JvmStatic
		val MATERIAL = CompletionAsset<Material>(sparkle.subKey("material"), false, listOf(InterchangeStructureInputRestriction.STRING)) {
			Material.values().withMap { name }
		}.doCheck {
			Material.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			tryOrNull { Material.valueOf(input) }
		}

		@JvmStatic
		val MATERIAL_VARIANT = CompletionAsset<Material>(sparkle.subKey("material_variant"), false, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			buildSet {
				DyeableMaterial.values().forEach { flex ->
					val key = flex.key().asString()
					add(key)
					add(flex.name)
					addAll(ColorType.values().withMap { "$key#$name" })
				}
			}
		}.doCheck {
			DyeableMaterial.materialFromMaterialCode(input) != null
		}.transformer {
			DyeableMaterial.materialFromMaterialCode(input)
		}

		@JvmStatic
		val MATERIAL_CODE = CompletionAsset<Material>(sparkle.subKey("material_codew"), false, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			buildSet {

				addAll(Material.values().withMap { key().asString() })

				DyeableMaterial.values().forEach { dyeable ->
					add(dyeable.key().asString())
					addAll(ColorType.values().withMap { "${dyeable.key().asString()}#$name" })
				}

			}
		}.doCheck {
			DyeableMaterial.materialFromMaterialCode(input) != null
		}.transformer {
			DyeableMaterial.materialFromMaterialCode(input)
		}

		@JvmStatic
		val EXECUTOR_HEALTH = CompletionAsset<Double>(sparkle.subKey("executor_health"), true, listOf(
			InterchangeStructureInputRestriction.DOUBLE)) {
			if (executor is Player) {
				listOf("${executor.health}")
			} else
				listOf("20.0")
		}.doCheck {
			if (executor is Player) input == "${executor.health}" else input == "20.0"
		}.transformer {
			input.toDouble()
		}

		@JvmStatic
		val EXECUTOR_LOCATION = CompletionAsset<Location>(sparkle.subKey("executor_location"), true, listOf(
			InterchangeStructureInputRestriction.STRING)) {
			if (executor is Player) {
				listOf(
					"@spawn",
					"@here",
					"@eyes",
					"@looking",
					"@bed",
					"@lastDamager",
					"@highestBlock",
					"@highestBlockAbove",
				)
			} else
				listOf("@spawn")
		}.doCheck {
			if (executor !is Player) input.equals("@spawn", ignoreCase) else setOf("spawn", "here", "eyes", "looking", "bed", "lastDamager", "highestBlock", "highestBlockBelow").any { input.equals("@$it", ignoreCase) }
		}.transformer {
			if (executor !is Player && input.equals("@spawn", ignoreCase)) {
				worlds[0].spawnLocation
			} else if (executor is Player) {
				when(input.removePrefix("@").lowercase()) {
					"spawn" -> worlds[0].spawnLocation
					"here" -> executor.location
					"eyes" -> executor.eyeLocation
					"looking" -> executor.rayTraceBlocks(100.0)?.hitBlock?.location ?: executor.eyeLocation
					"bed" -> executor.bedLocation
					"lastdamager" -> executor.lastDamageCause?.entity?.location ?: executor.location
					"highestblock" -> executor.location.toHighestLocation()
					"highestblockabove" -> executor.location.toHighestLocation().add(0.0, 1.0, 0.0)
					else -> null
				}
			} else
				null
		}

		@JvmStatic
		val LOADED_STRUCTURE = CompletionAsset<Structure>(sparkle.subKey("structure_loaded"), true) {
			Bukkit.getStructureManager().structures.map { it.key.asString() }
		}.doCheck {
			Bukkit.getStructureManager().structures.any { it.key.asString() == input }
		}.transformer {
			Bukkit.getStructureManager().structures.firstOrNull { it.key.asString() == input }?.value
		}

		@JvmStatic
		val STRUCTURE_ROTATION = CompletionAsset<StructureRotation>(sparkle.subKey("structure_rotation"), false) {
			StructureRotation.values().withMap { name }
		}.doCheck {
			StructureRotation.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			StructureRotation.values().firstOrNull { it.name.equals(input, ignoreCase) }
		}

		@JvmStatic
		val STRUCTURE_MIRROR = CompletionAsset<Mirror>(sparkle.subKey("structure_mirror"), false) {
			Mirror.values().withMap { name }
		}.doCheck {
			StructureRotation.values().any { it.name.equals(input, ignoreCase) }
		}.transformer {
			Mirror.values().firstOrNull { it.name.equals(input, ignoreCase) }
		}

		@JvmStatic
		fun pageCompletion(pages: () -> Number) = CompletionAsset<Long>(
			identityKey = sparkle.subKey("page"),
			refreshing = true,
			supportedInputType = listOf(InterchangeStructureInputRestriction.LONG),
			generator = {
				(1..pages().toLong()).mapToString()
			},
		)

		@JvmStatic
		fun files(path: Path, filter: (Path) -> Boolean = { true }, output: (String) -> String = { it }) = CompletionAsset<Path>(
			identityKey = sparkle.subKey("file"),
			refreshing = true,
			supportedInputType = listOf(InterchangeStructureInputRestriction.STRING),
			generator = {
				@OptIn(ExperimentalPathApi::class)
				path.walk().filter(filter).map { output(it.relativeToOrSelf(path).pathString) }.toList()
			},
		)

	}

}
