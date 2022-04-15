package de.moltenKt.jvm.application.configuration

import de.moltenKt.jvm.application.tag.Version
import de.moltenKt.jvm.application.tag.version
import de.moltenKt.jvm.tool.smart.identification.Identifiable

/**
 * This class represents the raw data of a MoltenKT application.
 * @param identity is the unique identifier of the application, used to manage configurations and resources.
 * @param version is the version of the application.
 * @author Fruxz
 * @since 1.0
 */
open class MoltenCoreApp(
	override val identity: String,
	open val version: Version = 1.0.version,
) : Identifiable<MoltenCoreApp>
