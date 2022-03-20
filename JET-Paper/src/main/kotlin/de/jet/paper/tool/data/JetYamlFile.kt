package de.jet.paper.tool.data

import de.jet.jvm.tool.smart.identification.Identifiable
import de.jet.paper.extension.paper.bukkitVersion
import de.jet.paper.structure.app.App
import de.jet.paper.structure.component.Component
import de.jet.paper.tool.smart.VendorsIdentifiable
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists

interface JetYamlFile : JetFile {

	companion object {

		private fun generateYaml(path: Path) =
			object : JetFile {

				override val file =
					path.apply {
						if (!parent.exists())
							parent.createDirectories()
						if (!exists())
							createFile()
					}

				val noPath = file.toFile()
				val yaml = YamlConfiguration.loadConfiguration(noPath)

				override fun load() {
					yaml.load(noPath)
				}

				override fun save() {
					yaml.save(noPath)
				}

				override fun contains(path: String) =
					yaml.contains(path)

				override fun <T : Any?> set(path: String, value: T) {
					yaml.set(path, value)
				}

				@Suppress("UNCHECKED_CAST")
				override fun <T> get(path: String): T? {
					val get = yaml.get(path)

					return try {
						get as T?
					} catch (e: ClassCastException) {
						null
					}

				}

			}

		fun appPath(vendor: Identifiable<App>) = Path("JETData", "#${vendor.identity}")

		fun appFile(
			vendor: Identifiable<App>,
			fileName: String,
			extension: String = "yml"
		) = generateYaml(appPath(vendor) / "$fileName.$extension")

		fun rootPath() =
			Path("JETData") / "ROOT"

		fun rootFile(fileName: String, extension: String = "yml") =
			generateYaml(rootPath() / "$fileName.$extension")

		fun componentPath(component: VendorsIdentifiable<Component>) =
			Path("JETData") / "#${component.identity}@${component.vendorIdentity.identity}"

		fun componentFile(component: VendorsIdentifiable<Component>, fileName: String, extension: String = "yml"): JetFile =
			generateYaml(componentPath(component) / "$fileName.$extension")

		fun versionPath(version: String = bukkitVersion) =
			Path("JETData") / version

		fun versionFile(fileName: String, extension: String = "yml") =
			generateYaml(versionPath() / "$fileName.$extension")

		internal fun dummyComponentFile(dataA: String, dataB: String, fileName: String, extension: String = "yml"): JetFile =
			generateYaml(Path("JETData") / "#$dataA@$dataB" / "$fileName.$extension")

	}

}