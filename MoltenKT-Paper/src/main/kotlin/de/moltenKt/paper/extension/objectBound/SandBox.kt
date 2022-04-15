package de.moltenKt.paper.extension.objectBound

import de.moltenKt.jvm.tool.timing.calendar.Calendar
import de.moltenKt.paper.app.MoltenCache.registeredSandBoxes
import de.moltenKt.paper.extension.mainLog
import de.moltenKt.paper.runtime.sandbox.SandBox
import de.moltenKt.paper.runtime.sandbox.SandBoxInteraction
import de.moltenKt.paper.structure.app.App
import java.util.logging.Level

@Suppress("NOTHING_TO_INLINE") // required, because of the Throwable
inline fun buildSandBox(vendor: App, identity: String, noinline action: suspend SandBoxInteraction.() -> Unit) =
	SandBox(vendor, identity, Calendar.now(), with(Throwable().stackTrace[0]) { "$className.$methodName()" }, action)

fun registerSandBox(sandBox: SandBox) {
	registeredSandBoxes.add(sandBox)
	sandBox.vendor.log.info("registering SandBox '${sandBox.identity}'!")
}

@Suppress("NOTHING_TO_INLINE") // required, because of the Throwable
inline fun buildAndRegisterSandBox(vendor: App, identity: String, noinline action: suspend SandBoxInteraction.() -> Unit) {
	registerSandBox(buildSandBox(vendor, identity, action))
}

val allSandBoxes: Set<SandBox>
	get() = registeredSandBoxes

fun getSandBox(fullIdentity: String) = registeredSandBoxes.firstOrNull { it.identity == fullIdentity }

fun destroySandBox(fullIdentity: String) = registeredSandBoxes.removeAll {
	(it.identity == fullIdentity).apply {
		if (this) {
			it.vendor.log.log(Level.INFO, "removing SandBox '$fullIdentity'!")
		}
	}
}

fun destroySandBox(sandBox: SandBox) =
	destroySandBox(sandBox.identity)

fun destroyAllSandBoxes() = registeredSandBoxes.clear()
	.also { mainLog(Level.WARNING, "WARNING! removing every SandBox!") }