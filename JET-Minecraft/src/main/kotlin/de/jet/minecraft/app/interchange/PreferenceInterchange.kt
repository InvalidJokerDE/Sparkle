package de.jet.minecraft.app.interchange

import de.jet.minecraft.app.JetCache
import de.jet.minecraft.extension.system
import de.jet.minecraft.structure.app.App
import de.jet.minecraft.structure.command.CompletionVariable
import de.jet.minecraft.structure.command.Interchange
import de.jet.minecraft.structure.command.InterchangeResult.SUCCESS
import de.jet.minecraft.structure.command.buildCompletion
import de.jet.minecraft.structure.command.isRequired
import de.jet.minecraft.structure.command.mustMatchOutput
import de.jet.minecraft.structure.command.next

class PreferenceInterchange(vendor: App = system) : Interchange(vendor, "preference", requiresAuthorization = true, completion = buildCompletion {
	next("list", "reset", "info") isRequired true mustMatchOutput true
	next(CompletionVariable.PREFERENCE) isRequired true mustMatchOutput true
}) {

	override var execution = execution {

		when {
			inputLength(1) && parameters.first() == "list" -> {
				""
			}
			inputLength(2) -> {
				val preference = JetCache.registeredPreferences.toList().firstOrNull { it.first.identity == parameters.first() }?.second

				if (preference != null) {
					when (parameters.first()) {
						"reset" -> {
							preference.reset()
						}
						"info" -> {

						}
					}
				} else
					TODO()
			}
		}

		SUCCESS
	}

}