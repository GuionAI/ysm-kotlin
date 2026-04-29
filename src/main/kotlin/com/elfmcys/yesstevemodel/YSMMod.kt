package com.elfmcys.yesstevemodel

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(YSMMod.MOD_ID)
object YSMMod {
    const val MOD_ID: String = "yesstevemodel"
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    init {
        LOGGER.info("YSM-Kotlin loading: mod_id={} kotlin_runtime={}", MOD_ID, KotlinVersion.CURRENT)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.elfmcys.yesstevemodel.client.YSMClientEvents.register()
        }
    }
}
