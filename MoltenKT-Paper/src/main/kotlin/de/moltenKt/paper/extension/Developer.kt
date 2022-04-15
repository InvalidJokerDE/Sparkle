package de.moltenKt.paper.extension

import de.moltenKt.jvm.tool.smart.identification.Identifiable
import de.moltenKt.jvm.tool.smart.identification.Identity
import de.moltenKt.jvm.tool.smart.positioning.Address
import de.moltenKt.paper.app.MoltenCache
import de.moltenKt.paper.runtime.app.LanguageSpeaker
import de.moltenKt.paper.runtime.lang.LanguageData
import de.moltenKt.paper.structure.app.App
import de.moltenKt.paper.structure.app.event.EventListener
import de.moltenKt.paper.structure.command.Interchange
import de.moltenKt.paper.structure.component.Component
import de.moltenKt.paper.tool.annotation.AutoRegister
import de.moltenKt.paper.tool.annotation.ExperimentalRegistrationApi
import de.moltenKt.paper.tool.smart.VendorOnDemand
import java.util.logging.Level
import kotlin.reflect.full.hasAnnotation

fun <T : Any?> T.debugLog(message: String) = this.also {
	if (de.moltenKt.paper.app.MoltenApp.debugMode) {
		message.lines().forEach { line ->
			mainLog(Level.WARNING, "[DEBUG] $line")
		}
	}
}

fun mainLog(level: Level = Level.INFO, message: String) = de.moltenKt.paper.app.MoltenApp.instance.log.log(level, message)

val mainLog = de.moltenKt.paper.app.MoltenApp.instance.log

internal val lang: LanguageSpeaker
	get() = de.moltenKt.paper.MoltenEngine.languageSpeaker

// TODO: 12.10.21 LanguageSpeaker bei Address austauschen mit sinnvoller, erst dann existierender Klasse
fun getSystemTranslated(@Suppress("UNUSED_PARAMETER") vendor: Identifiable<out App>, address: Address<LanguageData>): String {
	return lang(id = address.addressString) //todo replace with real system
}

operator fun LanguageSpeaker.get(id: String) = lang(id)

internal fun lang(id: String, smartColor: Boolean = true, basicHTML: Boolean = true) = lang.message(id, smartColor, basicHTML)

internal val system: de.moltenKt.paper.app.MoltenApp
	get() = de.moltenKt.paper.MoltenEngine.appInstance

@Throws(NoSuchElementException::class)
fun app(id: String) = MoltenCache.registeredApplications.first { it.appIdentity == id }

@Throws(NoSuchElementException::class)
fun app(vendor: Identifiable<out App>) = MoltenCache.registeredApplications.first { it.appIdentity == vendor.identity }

@Throws(NoSuchElementException::class)
fun app(vendorIdentity: Identity<out App>) = MoltenCache.registeredApplications.first { it.appIdentity == vendorIdentity.identity }

@Throws(NoSuchElementException::class)
fun Identifiable<out App>.getApp() = app(identity)

@Suppress("FINAL_UPPER_BOUND")
@OptIn(ExperimentalRegistrationApi::class)
fun <T : VendorOnDemand> T.runIfAutoRegister() {
	if (this::class.hasAnnotation<AutoRegister>()) {
		if (preferredVendor != null) {

			when (this) {

				is Component -> {
					MoltenCache.initializationProcesses.add { preferredVendor?.add(this) }
					debugLog(this::class.simpleName + " added to Component-AutoRegister")
				}

				is Interchange -> {
					MoltenCache.initializationProcesses.add { preferredVendor?.add(this) }
					debugLog(this::class.simpleName + " added to Interchange-AutoRegister")
				}

				is EventListener -> {
					MoltenCache.initializationProcesses.add { preferredVendor?.add(this) }
					debugLog(this::class.simpleName + " added to EventListener-AutoRegister")
				}

			}

		}
	}
}