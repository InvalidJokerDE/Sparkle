package de.jet.paper.app.interchange

import de.jet.jvm.extension.collection.replace
import de.jet.jvm.extension.collection.replaceVariables
import de.jet.paper.app.JetCache
import de.jet.paper.app.JetData
import de.jet.paper.extension.display.message
import de.jet.paper.extension.display.notification
import de.jet.paper.extension.get
import de.jet.paper.extension.lang
import de.jet.paper.structure.command.Interchange
import de.jet.paper.structure.command.InterchangeResult
import de.jet.paper.structure.command.InterchangeResult.SUCCESS
import de.jet.paper.structure.command.completion.buildInterchangeStructure
import de.jet.paper.structure.command.completion.component.CompletionAsset
import de.jet.paper.structure.command.completion.component.CompletionComponent
import de.jet.paper.structure.command.completion.component.CompletionComponent.Companion
import de.jet.paper.structure.command.live.InterchangeAccess
import de.jet.paper.tool.display.message.Transmission.Level.FAIL
import de.jet.paper.tool.display.message.Transmission.Level.INFO

class ComponentInterchange : Interchange(
	label = "component",
	completion = buildInterchangeStructure {
		branch {
			content(CompletionComponent.static("list"))
		}
		branch {
			content(Companion.static("start", "stop", "autostart"))
			branch {
				content(Companion.asset(CompletionAsset.COMPONENT))
			}
		}
	}
) {
	override val execution: InterchangeAccess.() -> InterchangeResult = {

		if (parameters.size == 1 && parameters.first() == "list") {

			mutableListOf(lang["interchange.internal.component.list.header"]).apply {
				add(
					lang["interchange.internal.component.list.description"].replaceVariables(
						"1" to "⏻/⭘",
						"2" to "⚡",
					)
				)
				JetCache.registeredComponents.forEach { component ->
					add(
						lang("interchange.internal.component.list.line")
							.replace(
								"[component]" to component.identity,
								"[autoStart]" to if (component.isAutoStarting || JetData.autoStartComponents.content.contains(
										component.identity
									)
								) "§a§o⚡" else "§c§o⚡",
								"[status]" to if (component.isRunning) "§a⏻" else "§c⭘"
							)
					)
				}
			}.forEach {
				it.message(executor).display()
			}

			SUCCESS

		} else if (parameters.size == 2) {

			try {

				val component = JetCache.registeredComponents.first { it.identity == parameters.last() }

				when (parameters.first().lowercase()) {

					"start" -> {

						if (!component.isRunning) {

							component.vendor.start(component.identityObject)

							lang("interchange.internal.component.nowRunning")
								.replace("[component]", component.identity)
								.notification(INFO, executor).display()

						} else
							lang("interchange.internal.component.alreadyRunning")
								.replace("[component]", component.identity)
								.notification(FAIL, executor).display()

						SUCCESS
					}

					"stop" -> {
						if (component.isRunning) {

							component.vendor.stop(component.identityObject)

							lang("interchange.internal.component.nowStopped")
								.replace("[component]", component.identity)
								.notification(INFO, executor).display()

						} else
							lang("interchange.internal.component.missingRunning")
								.replace("[component]", component.identity)
								.notification(FAIL, executor).display()

					}

					"autostart" -> {

						if (!component.canBeAutoStartToggled) {

							JetData.autoStartComponents.let { preference ->
								val currentState = preference.content.toMutableSet()

								lang(
									if (currentState.contains(component.identity)) {
										currentState.remove(component.identity)
										"interchange.internal.component.autoStartRemoved"
									} else {
										currentState.add(component.identity)
										"interchange.internal.component.autoStartAdded"
									}
								)
									.replace("[component]", component.identity)
									.notification(FAIL, executor).display()

								preference.content = currentState
							}

						} else
							lang("interchange.internal.component.autoStartStatic")
								.replace("[component]", component.identity)
								.notification(FAIL, executor).display()

					}

				}

				SUCCESS

			} catch (e: NoSuchElementException) {
				InterchangeResult.WRONG_USAGE
			}

		} else
			InterchangeResult.WRONG_USAGE

	}
}