package net.leafmc.chainharvest

import org.bukkit.block.Block
import org.bukkit.entity.Player

enum class ProtectionDecision {
    ALLOW,
    DENY,
    NO_ADAPTER,
}

fun interface ProtectionAdapter {
    fun canMutate(player: Player, block: Block, action: ProtectionAction): ProtectionDecision
}

class ProtectionGate(
    private val settingsProvider: () -> ProtectionSettings,
    private val residenceAdapter: ProtectionAdapter?,
) {
    fun canMutate(player: Player, block: Block, action: ProtectionAction): ProtectionDecision {
        val settings = settingsProvider()
        if (residenceAdapter != null) {
            val decision = residenceAdapter.canMutate(player, block, action)
            if (decision != ProtectionDecision.NO_ADAPTER) {
                return decision
            }
        }
        return ProtectionPolicy.fallbackDecision(settings)
    }
}

object ProtectionPolicy {
    fun fallbackDecision(settings: ProtectionSettings): ProtectionDecision {
        if (!settings.requireResidenceForFarmActions || settings.unsafeFallbackWithoutResidence) {
            return ProtectionDecision.ALLOW
        }
        return ProtectionDecision.DENY
    }
}
