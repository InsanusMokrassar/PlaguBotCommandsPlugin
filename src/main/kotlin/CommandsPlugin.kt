package dev.inmo.plagubot.plugins.commands

import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.api.bot.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * This plugin has been created for centralized work with commands in context of [Plugin]s system of plagubot. Pass
 * [BotCommandFullInfo] in your [Plugin.setupDI] section to declare some command. You may use [CommandsKeeper] for
 * flexible setup of commands in runtime.
 */
@Serializable
class CommandsPlugin : Plugin {
    private val log = KSLog(logTag)

    /**
     * Creating [CommandsKeeper] and pass it to the DI. It uses [org.koin.core.scope.Scope.getAll] to get all the
     * [BotCommandFullInfo] instances declared in the DI.
     */
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { CommandsKeeper(getAll<BotCommandFullInfo>().distinct()) }
    }

    private suspend fun BehaviourContext.setScopeCommands(key: CommandsKeeperKey, commands: List<BotCommand>?) {
        runCatchingSafely {
            commands ?.let {
                setMyCommands(
                    commands.distinctBy { it.command },
                    key.scope,
                    key.languageCode
                )
            } ?: deleteMyCommands(
                key.scope,
                key.languageCode
            )
        }.onFailure {
            log.e {
                "Unable to ${if (commands == null) "delete commands" else "set new commands (${commands.joinToString { it.command }})"} for key $key"
            }
        }.onSuccess {
            log.i {
                "Successfully ${if (commands == null) "deleted commands" else "set new commands (${commands.joinToString { it.command }})"} for key $key"
            }
        }
    }

    /**
     * Uses [CommandsKeeper] from [koin]. Subscribe on [CommandsKeeper.scopesCommands] to follow changed in scopes and
     * take all the available keys in the [CommandsKeeper] and set commands for each key
     */
    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val commandsKeeper = koin.commandsKeeper

        log.d { "Subscribe to scopes changed flow" }
        commandsKeeper.onScopeChanged.subscribeSafelyWithoutExceptions(scope) {
            val commands = commandsKeeper.getCommands(it)
            setScopeCommands(it, commands)
        }
        log.d { "Subscribed to scopes changed flow" }

        log.d { "Start setup initially passed commands" }
        commandsKeeper.getKeys().forEach {
            val commands = commandsKeeper.getCommands(it)
            log.d { "Start setup initially passed commands for key $it: ${commands ?.joinToString { it.command }}" }
            setScopeCommands(it, commandsKeeper.getCommands(it))
        }
        log.d { "Complete setup initially passed commands" }
    }
}
