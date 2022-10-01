package de.moltenKt.paper.tool.display.item.action

import de.fruxz.ascend.tool.timing.calendar.Calendar
import de.moltenKt.paper.app.MoltenCache
import de.moltenKt.paper.runtime.event.interact.PlayerInteractAtItemEvent
import de.moltenKt.paper.tool.display.item.action.ItemActionType.INTERACT

class ItemInteractAction(
    override val identity: String,
    override val type: ItemActionType = INTERACT,
    override val executionProcess: PlayerInteractAtItemEvent.() -> Unit,
    override val created: Calendar = Calendar.now(),
) : ItemAction<PlayerInteractAtItemEvent> {

    override fun register() { MoltenCache.itemActions += this }

    override fun unregister() { MoltenCache.itemActions -= this }

    override fun isRegistered() = MoltenCache.itemActions.contains(this)

}