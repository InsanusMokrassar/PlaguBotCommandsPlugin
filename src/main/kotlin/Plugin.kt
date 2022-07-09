package plagubot_plugin

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.logTag
import dev.inmo.kslog.common.d
import dev.inmo.kslog.common.dS
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

// Left empty constructor for the plugin instantiation in bot
@Serializable
class PlaguBotPlugin : Plugin {
    private val logger = KSLog(logTag)

    @Serializable
    data class Config(
        val someParam: String = "Default value"
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        // Here you may declare any dependencies you need
        single {
            // Take from config value by field "plugin". Replace to your plugin name and fill Config with your plugin configuration
            get<Json>().decodeFromJsonElement(Config.serializer(), params["plugin"] ?: return@single null)
        }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        // Here you may configure the behaviour of your plugin
        // All dependencies available in bot can be accessed via koin
        logger.d { koin.get<Config>().someParam }
        logger.dS { getMe().toString() }
        onCommand("hello_world") {
            reply(it, "Hello :)")
        }
    }
}
