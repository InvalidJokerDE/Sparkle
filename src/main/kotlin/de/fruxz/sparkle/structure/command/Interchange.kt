package de.fruxz.sparkle.structure.command

import de.fruxz.ascend.extension.catchException
import de.fruxz.ascend.extension.empty
import de.fruxz.ascend.tool.smart.identification.Identity
import de.fruxz.sparkle.extension.debugLog
import de.fruxz.sparkle.extension.display.BOLD
import de.fruxz.sparkle.extension.display.notification
import de.fruxz.sparkle.extension.interchange.InterchangeExecutor
import de.fruxz.sparkle.extension.interchange.Parameters
import de.fruxz.sparkle.extension.paper.asPlayer
import de.fruxz.sparkle.extension.paper.asPlayerOrNull
import de.fruxz.sparkle.extension.timing.RunningCooldown
import de.fruxz.sparkle.extension.timing.getCooldown
import de.fruxz.sparkle.extension.timing.hasCooldown
import de.fruxz.sparkle.extension.timing.setCooldown
import de.fruxz.sparkle.structure.app.App
import de.fruxz.sparkle.structure.command.InterchangeAuthorizationType.MOLTEN
import de.fruxz.sparkle.structure.command.InterchangeResult.*
import de.fruxz.sparkle.structure.command.InterchangeUserRestriction.*
import de.fruxz.sparkle.structure.command.completion.InterchangeStructure
import de.fruxz.sparkle.structure.command.completion.emptyInterchangeStructure
import de.fruxz.sparkle.structure.command.live.InterchangeAccess
import de.fruxz.sparkle.tool.annotation.LegacyCraftBukkitFeature
import de.fruxz.sparkle.tool.display.message.Transmission.Level
import de.fruxz.sparkle.tool.display.message.Transmission.Level.ERROR
import de.fruxz.sparkle.tool.permission.Approval
import de.fruxz.sparkle.tool.smart.ContextualInstance
import de.fruxz.sparkle.tool.smart.Labeled
import de.fruxz.sparkle.tool.smart.Logging
import de.fruxz.sparkle.tool.smart.VendorOnDemand
import de.fruxz.stacked.buildComponent
import de.fruxz.stacked.extension.KeyingStrategy.CONTINUE
import de.fruxz.stacked.extension.asComponent
import de.fruxz.stacked.extension.dyeGray
import de.fruxz.stacked.extension.dyeRed
import de.fruxz.stacked.extension.dyeYellow
import de.fruxz.stacked.extension.style
import de.fruxz.stacked.extension.subKey
import de.fruxz.stacked.hover
import de.fruxz.stacked.plus
import de.fruxz.stacked.text
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.WARNING
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * @param label is the interchange name, used as /<name> and as the identifier, defined inside your plugin.yml
 * @param aliases are the supported interchange-aliases, which can be used, to access the interchange
 * @param protectedAccess defines, if the [InterchangeExecutor] requires to have the permission 'interchange.<label>' to execute this interchange
 * @param userRestriction defines, which types of [InterchangeExecutor] are allowed, to execute the interchange
 * @param accessProtectionType defines the system, which is used, to check the [InterchangeExecutor]s ability to execute the interchange
 * @param hiddenFromRecommendation if enabled, this interchange will not be visible in the tab-recommendations of usable commands
 * @param completion defines the completions, that the interchange will display, during a [InterchangeExecutor]s input into console/chat
 * @param ignoreInputValidation defines, if the input of the user should not be checked of correctness
 * @param preferredVendor if not null, this overrides the automatic extrapolated [vendor] [App] on registering
 * @author Fruxz
 * @since 1.0
 */
abstract class Interchange(
	final override val label: String,
	val aliases: Set<String> = emptySet(),
	val protectedAccess: Boolean = false,
	val userRestriction: InterchangeUserRestriction = NOT_RESTRICTED,
	val accessProtectionType: InterchangeAuthorizationType = MOLTEN,
	val hiddenFromRecommendation: Boolean = false,
	val completion: InterchangeStructure<out InterchangeExecutor> = emptyInterchangeStructure(),
	val ignoreInputValidation: Boolean = false,
	var forcedApproval: Approval? = null,
	final override val preferredVendor: App? = null,
	val cooldown: Duration = Duration.ZERO
) : CommandExecutor, ContextualInstance<Interchange>, VendorOnDemand, Logging, Labeled {

	init {
		completion.identity = label

		preferredVendor?.let {
			vendor = it
		}

	}

	/**
	 * The [vendor] of this interchange, represents the [App],
	 * owning / running this interchange.
	 * @author Fruxz
	 * @since 1.0
	 */
	final override lateinit var vendor: App
		private set

	override val identityKey by lazy { vendor.subKey(label.lowercase(), CONTINUE) }

	/**
	 * This function replaces the current [vendor] of this [Interchange]
	 * with the [newVendor].
	 * This only happens, if the current [vendor] is not set (not initialized),
	 * or if [override] is true *(default: false)*.
	 * @param newVendor the new vendor, which will be used
	 * @param override defines, if the old vendor (if set) will be replaced with [newVendor]
	 * @return If the vendor-change happens, true is returned, otherwise false is returned!
	 * @author Fruxz
	 * @since 1.0
	 */
	override fun replaceVendor(newVendor: App, override: Boolean) = if (override || !this::vendor.isInitialized) {
		vendor = newVendor
		true
	} else
		false

	/**
	 * This value represents a completely generated / computed completion, which
	 * get used, to display the user good input recommendations, during the input.
	 * This tabCompleter is a **not** a computational value, but it contains code,
	 * that compute its recommendations adaptively to the input and to the changes
	 * of the containing assets.
	 * The TabCompleter accesses the [completion] and uses its [InterchangeStructure.computeCompletion]
	 * function, to compute the current state of adaptive and most fitting recommendations.
	 * By doing so, the TabCompleter produces new results at every tab-completion call,
	 * but the tab-completer itself, stays same, without changing it.
	 * @see InterchangeStructure.computeCompletion
	 * @author Fruxz
	 * @since 1.0
	 */
	val tabCompleter = TabCompleter { executor, _, _, args ->
		completion.computeCompletion(args.toList(), executor).takeIf { canExecuteBasePlate(executor) } ?: listOf(" ")
	}

	/**
	 * This value represents the thread context (Kotlin Coroutines) of
	 * this interchange. It is used, to process and execute the actions
	 * of this interchange, so that a user cannot freeze the whole server,
	 * by only using an interchange, that processes simple stuff.
	 * The [threadContext] basically uses the [newSingleThreadContext]
	 * function and uses the [identity] of this interchange as the name
	 * of the bound thread.
	 * @author Fruxz
	 * @since 1.0
	 */
	override val threadContext by lazy {
		@OptIn(DelicateCoroutinesApi::class)
		newSingleThreadContext(identity)
	}

	/**
	 * This value is the section-name of the logger, attached to
	 * this [Interchange]-Object.
	 * @see Logging
	 * @author Fruxz
	 * @since 1.0
	 */
	final override val sectionLabel = "InterchangeEngine"

	/**
	 * This value is the second-part of the identity of this [Interchange],
	 * used to track and identify this [Interchange], among the other
	 * [Interchange]s, used by your or other [App]s.
	 * @see ContextualInstance
	 * @author Fruxz
	 * @since 1.0
	 */
	override val thisIdentity = label

	/**
	 * This value is the first-part of the identity of this [Interchange],
	 * used to track and identify this [Interchange], among the other
	 * [Interchange]s, used by your or other [App]s.
	 * @see ContextualInstance
	 * @author Fruxz
	 * @since 1.0
	 */
	final override val vendorIdentity: Identity<App>
		get() = vendor.identityObject

	/**
	 * This computed [lazy]-initialization value represents the required [Approval],
	 * to be able to access this [Interchange] as a [InterchangeExecutor].
	 * This value is computed from the [protectedAccess], [vendor] and [label]
	 * properties, to generate the [Approval].
	 * This value can be null, if this [Interchange] does not have any [Approval]-
	 * Requirements, to access this [Interchange], otherwise, the contained [Approval]-
	 * Value is required, to use this interchange!
	 * **if [forcedApproval] is not null (null is default), then the [forcedApproval]
	 * is returned, instead of the computed value!**
	 * @author Fruxz
	 * @since 1.0
	 */
	val requiredApproval by lazy {
		forcedApproval ?: Approval.fromApp(vendor, "interchange.$label").takeIf { protectedAccess }
	}

	// parameters

	/**
	 * This abstract value defines the execution block, which will be executed, after a
	 * [InterchangeExecutor]s input, the [Approval] checks (based on [requiredApproval])
	 * and the completion-Checks (based on [completion]).
	 * This execution block is a **suspend** block, because it gets executed, from the
	 * [vendor]s [App.coroutineScope], at a [threadContext] context.
	 * To easily create your execution block, we recommend you, to use the [execution]
	 * function!
	 * @see execution
	 * @author Fruxz
	 * @since 1.0
	 */
	abstract val execution: suspend InterchangeAccess<out InterchangeExecutor>.() -> InterchangeResult

	// runtime-functions

	private fun interchangeException(
		exception: Exception,
		executor: InterchangeExecutor,
		executorType: InterchangeUserRestriction
	) {
		sectionLog.log(
			WARNING,
			"Executor ${executor.name} as ${executorType.name} caused an error at execution at ${with(exception.stackTrace[0]) { "$className:$methodName" }}!"
		)
	}

	/**
	 * If the [executor] can execute the base of the interchange
	 * with its approvals. (**Not looking for the parameters and
	 * its own possible approvals!**)
	 */
	private fun canExecuteBasePlate(executor: InterchangeExecutor) =
		accessProtectionType != MOLTEN || requiredApproval == null || requiredApproval?.hasApproval(executor) ?: true

	private fun wrongApprovalFeedback(
		receiver: InterchangeExecutor,
	) {

		text {
			this + text("You currently do ").dyeGray()
			this + text("not").dyeRed()
			this + text(" have the ").dyeGray()
			this + text("required approval").dyeYellow().hover {
				text("Required Approval: ").dyeGray()
				text("${requiredApproval?.identity}").dyeYellow()
			}
			this + text(" to execute this interchange!").dyeGray()
		}.notification(Level.FAIL, receiver).display()

	}

	private fun wrongUsageFeedback(
		receiver: InterchangeExecutor,
	) {

		text {
			this + text("Follow the ").dyeGray()
			this + text("syntax").dyeRed()
			this + text(", to execute this! See:").dyeGray()
		}.notification(Level.FAIL, receiver).display()

		receiver.sendMessage(Component.text(completion.buildSyntax(receiver), NamedTextColor.YELLOW))
	}

	private fun wrongClientFeedback(
		receiver: InterchangeExecutor,
	) {

		text {
			this + text("This action ").dyeGray()
			this + text("requires").dyeRed()
			this + text(" you as a '").dyeGray()
			this + text(userRestriction.name).dyeYellow()
			this + text("', to be executed!").dyeGray()
		}.notification(Level.FAIL, receiver).display()

	}

	private fun issueFeedback(
		receiver: InterchangeExecutor
	) {

		text {
			this + text("Oops!").style(NamedTextColor.RED, BOLD)
			this + text(" A").dyeGray()
			this + text(" critical error ").dyeRed()
			this + text("occurred, while executing this interchange!").dyeGray()
		}.notification(ERROR, receiver).display()

	}

	internal fun cooldownFeedback(
		receiver: InterchangeExecutor,
		cooldown: RunningCooldown?,
	) {
		buildComponent {
			this + "You have to wait ".asComponent.color(NamedTextColor.GRAY)
			this + ((cooldown?.remaining?.toString(DurationUnit.SECONDS, 0) ?: "") + " ").asComponent
				.style(Style.style(NamedTextColor.RED, BOLD))
				.hover {
					cooldown?.destination?.getFormatted(receiver.asPlayer.locale())?.asComponent?.color(NamedTextColor.GRAY)
				}
			this + "until you can execute this (sub-)interchange again!".asComponent.color(NamedTextColor.GRAY)
		}.notification(Level.FAIL, receiver).display()
	}

	// logic

	override fun onCommand(sender: InterchangeExecutor, command: Command, label: String, args: Parameters): Boolean {

		vendor.coroutineScope.launch(context = threadContext) {

			val parameters = args.toList()
			val executionProcess = this@Interchange::execution

			if (canExecuteBasePlate(sender)) {

				if (sender is ConsoleCommandSender || !cooldown.isPositive() || sender.asPlayerOrNull?.hasCooldown("interchange:$key") != true) {

					if (
						(userRestriction == NOT_RESTRICTED)
						|| (sender is Player && userRestriction == ONLY_PLAYERS)
						|| (sender is ConsoleCommandSender && userRestriction == ONLY_CONSOLE)
					) {

						if (ignoreInputValidation || completion.validateInput(parameters, sender)) {
							val clientType = if (sender is Player) ONLY_PLAYERS else ONLY_CONSOLE

							fun exception(exception: Exception) {
								sectionLog.log(
									WARNING,
									"Executor ${sender.name} as ${clientType.name} caused an error at execution of $label-command!"
								)
								catchException(exception)
							}

							try {

								when (executionProcess()(
									InterchangeAccess(
										vendor,
										clientType,
										sender,
										this@Interchange,
										label,
										parameters,
										emptyList()
									)
								)) {

									NOT_PERMITTED -> wrongApprovalFeedback(sender)
									WRONG_CLIENT -> wrongClientFeedback(sender)
									WRONG_USAGE -> wrongUsageFeedback(sender)
									FAIL -> issueFeedback(sender)
									BRANCH_COOLDOWN -> empty()
									SUCCESS -> {
										sender.asPlayerOrNull?.setCooldown("interchange:$key", cooldown)
										debugLog("Executor ${sender.name} as ${clientType.name} successfully executed $label-interchange!")
									}

								}

							} catch (e: Exception) {
								issueFeedback(sender)
								exception(e)
							} catch (e: java.lang.Exception) {
								issueFeedback(sender)
								exception(e)
							} catch (e: NullPointerException) {
								issueFeedback(sender)
								exception(e)
							} catch (e: NoSuchElementException) {
								issueFeedback(sender)
								exception(e)
							}

						} else
							wrongUsageFeedback(sender)

					} else
						wrongClientFeedback(sender)

				} else
					cooldownFeedback(sender, sender.asPlayer.getCooldown("interchange:$key"))

			} else
				wrongApprovalFeedback(sender)

		}

		return true
	}

}

enum class InterchangeResult {

	SUCCESS, NOT_PERMITTED, WRONG_USAGE, WRONG_CLIENT, FAIL, BRANCH_COOLDOWN;

}

enum class InterchangeUserRestriction {

	ONLY_PLAYERS,
	ONLY_CONSOLE,
	NOT_RESTRICTED;

	fun match(sender: InterchangeExecutor): Boolean {
		return when (this) {
			ONLY_PLAYERS -> sender is Player
			ONLY_CONSOLE -> sender is ConsoleCommandSender
			NOT_RESTRICTED -> true
		}
	}

}

enum class InterchangeAuthorizationType {

	MOLTEN,

	@LegacyCraftBukkitFeature
	CRAFTBUKKIT,

	NONE;

}

@Suppress("unused") // todo use Interchange as context, when the kotlin context API is ready
fun Interchange.execution(execution: suspend InterchangeAccess<out InterchangeExecutor>.() -> InterchangeResult) =
	execution