package com.elfmcys.yesstevemodel

import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(YSMMod.MOD_ID)
object YSMMod {
    const val MOD_ID: String = "yesstevemodel"
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    init {
        LOGGER.info("YSM-Kotlin loading: mod_id={} kotlin_runtime={}", MOD_ID, KotlinVersion.CURRENT)
    }
}
