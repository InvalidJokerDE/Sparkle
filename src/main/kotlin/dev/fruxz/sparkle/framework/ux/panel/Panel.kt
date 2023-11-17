package dev.fruxz.sparkle.framework.ux.panel

import dev.fruxz.ascend.extension.forceCast
import dev.fruxz.ascend.tool.map.list.MutableListMap
import dev.fruxz.ascend.tool.smart.identity.RelatedIdentity
import dev.fruxz.ascend.tool.smart.identity.RelatedUniq
import dev.fruxz.sparkle.framework.coroutine.dispatcher.asyncDispatcher
import dev.fruxz.sparkle.framework.system.sparkle
import dev.fruxz.sparkle.framework.ux.inventory.container.buildInventory
import dev.fruxz.sparkle.framework.ux.inventory.container.set
import dev.fruxz.sparkle.framework.ux.inventory.item.ItemLike
import dev.fruxz.stacked.extension.asPlainString
import kotlinx.coroutines.Deferred
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.minecraft.world.inventory.ClickAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import java.util.*
import kotlin.coroutines.CoroutineContext

open class Panel(
    open val label: ComponentLike = Component.empty(),
    open val format: PanelFormat = PanelFormat.ofLines(PanelFormat.InventoryLines(3)),
    open val flags: Set<PanelFlag> = emptySet(),
    open val sound: PanelSound = PanelSound(onOpen = null, onClose = null),
    open val content: Map<Int, ItemLike> = emptyMap(),
    open val lazyContent: Map<Int, Deferred<ItemLike>> = emptyMap(),
    open val displayContext: CoroutineContext = sparkle.asyncDispatcher,
    open val updateContext: CoroutineContext = sparkle.asyncDispatcher,
    open val uuid: UUID = UUID.randomUUID(),
) : InventoryUI, RelatedUniq<Panel, UUID>, Keyed {

    @JvmInline
    value class ClickAction(val action: suspend (InventoryClickEvent) -> Unit)

    internal open val clickActions = MutableListMap<Int, ClickAction>()

    override fun key(): Key = Key.key("sparkle", uuid.toString())

    /**
     * Generates a string, which can be used to identify this panel.
     */
    fun identifier(): String = "'${label.asPlainString}'@${key()}"

    override val identity: RelatedIdentity<Panel, UUID> by lazy {
        RelatedIdentity(uuid)
    }

    override fun produce(): Inventory {
        val holder = PanelHolder(this)
        val inventory = when (format) {
            is PanelFormat.SizePanelFormat -> buildInventory(format.forceCast<PanelFormat.SizePanelFormat>().size.size, label, holder)
            is PanelFormat.LinesPanelFormat -> buildInventory(format.forceCast<PanelFormat.LinesPanelFormat>().lines.lines * 9, label, holder)
            is PanelFormat.TypePanelFormat -> buildInventory(format.forceCast<PanelFormat.TypePanelFormat>().type, label, holder)
        }

        content.forEach(inventory::set)

        return inventory
    }

    fun toMutablePanel() = MutablePanel(
        label = label,
        format = format,
        flags = flags.toMutableSet(),
        sound = sound,
        content = content.toMutableMap(),
        lazyContent = lazyContent.toMutableMap(),
        displayContext = displayContext,
        updateContext = updateContext,
    )

}
