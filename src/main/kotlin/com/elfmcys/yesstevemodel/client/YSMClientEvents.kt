package com.elfmcys.yesstevemodel.client

import com.elfmcys.yesstevemodel.YSMMod
import com.elfmcys.yesstevemodel.client.render.YSMRenderBridge
import com.elfmcys.yesstevemodel.model.YSMModelManager
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod.EventBusSubscriber(modid = YSMMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object YSMClientEvents {

    fun register() {
        MOD_BUS.register(this)
    }

    @SubscribeEvent
    fun onRegisterClientReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { rm: ResourceManager ->
            YSMModelManager.reload(rm)
            YSMRenderBridge.onModelsReloaded()
        })
    }
}
