package net.leafmc.chainharvest

import com.bekvon.bukkit.residence.api.ResidenceApi
import com.bekvon.bukkit.residence.containers.Flags
import org.bukkit.block.Block
import org.bukkit.entity.Player

class ResidenceProtectionGate : ProtectionAdapter {
    override fun canMutate(player: Player, block: Block, action: ProtectionAction): ProtectionDecision {
        return try {
            val residence = ResidenceApi.getResidenceManager().getByLoc(block.location) ?: return ProtectionDecision.ALLOW
            val allowed = residence.permissions.playerHas(player, Flags.build, true)
            if (allowed) ProtectionDecision.ALLOW else ProtectionDecision.DENY
        } catch (_: LinkageError) {
            ProtectionDecision.NO_ADAPTER
        } catch (_: RuntimeException) {
            ProtectionDecision.NO_ADAPTER
        }
    }
}
