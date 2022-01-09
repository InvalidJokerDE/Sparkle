package de.jet.paper.extension.scoreboard

import de.jet.paper.extension.paper.getOfflinePlayer
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

/**
 * This value returns every entry from the [Team.getEntries] function, mapped to a [OfflinePlayer] object.
 * @return [List]<[OfflinePlayer]> every entry from the [Team.getEntries] function, mapped to a [OfflinePlayer] object.
 * @see Team.getEntries
 * @author Fruxz
 * @since 1.0
 */
val Team?.entriesAsOfflinePlayer: List<OfflinePlayer>
    get() = this?.entries?.map { getOfflinePlayer(it) } ?: emptyList()

/**
 * This value returns every entry from the [entriesAsOfflinePlayer] function, mapped to a [Player] object.
 * But only contained from the [entriesAsOfflinePlayer], if the [OfflinePlayer] is currently online.
 * @see entriesAsOfflinePlayer
 * @return [List]<[Player]> every entry from the [entriesAsOfflinePlayer] function, mapped to a [Player] object.
 * @author Fruxz
 * @since 1.0
 */
val Team?.validOnlinePlayers: List<Player>
    get() = this?.entriesAsOfflinePlayer?.filter { it.isOnline }?.map { it.player!! } ?: emptyList()