package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class CoinCounterClient {

    private CoinCounterClient() {
    }
}
