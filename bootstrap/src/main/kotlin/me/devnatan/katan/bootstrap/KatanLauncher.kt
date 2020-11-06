package me.devnatan.katan.bootstrap

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.*
import me.devnatan.katan.api.KatanEnvironment
import me.devnatan.katan.api.defaultLogLevel
import me.devnatan.katan.cli.KatanCLI
import me.devnatan.katan.common.exceptions.SilentException
import me.devnatan.katan.common.util.exportResource
import me.devnatan.katan.common.util.get
import me.devnatan.katan.core.KatanCore
import me.devnatan.katan.core.KatanCore.Companion.DEFAULT_VALUE
import me.devnatan.katan.core.KatanLocale
import me.devnatan.katan.webserver.KatanWS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

private class KatanLauncher(config: Config, environment: KatanEnvironment, locale: KatanLocale) {

    companion object {

        const val ENV_PROPERTY = "katan.environment"
        const val LOCALE_ENV_PROPERTY = "katan.locale"
        const val LOG_LEVEL_ENV_PROPERTY = "katan.log.level"
        const val TRANSLATION_FILE_PATTERN = "translations/%s.properties"
        const val FALLBACK_LANGUAGE = "en"

        private val logger: Logger by lazy {
            LoggerFactory.getLogger(KatanLauncher::class.java)
        }

        @JvmStatic
        fun main(args: Array<out String>) {
            val env = System.getProperty(ENV_PROPERTY, KatanEnvironment.LOCAL).toLowerCase()
            if (env !in KatanEnvironment.ALL)
                return System.err.println(
                    "Environment \"$env\" is not valid for Katan. You can only choose these: ${
                        KatanEnvironment.ALL.joinToString(
                            ", "
                        )
                    }"
                )

            val katanEnv = KatanEnvironment(env)
            System.setProperty(LOG_LEVEL_ENV_PROPERTY, katanEnv.defaultLogLevel().toString())

            var config = ConfigFactory.parseFile(exportResource("katan.conf"))

            val environmentConfig = File("katan.$env.conf")
            if (environmentConfig.exists()) {
                config = ConfigFactory.parseFile(environmentConfig).withFallback(config)
            } else {
                val localConfig = File("katan.local.conf")
                if (localConfig.exists())
                    config = ConfigFactory.parseFile(localConfig).withFallback(config)
            }

            var userLocale: Locale = if (config.get("locale", DEFAULT_VALUE) == DEFAULT_VALUE)
                Locale.getDefault()
            else
                Locale.forLanguageTag(config.get("locale", FALLBACK_LANGUAGE))

            val messages = runCatching {
                exportResource(TRANSLATION_FILE_PATTERN.format(userLocale.toLanguageTag()))
            }.getOrElse {
                runCatching {
                    exportResource(TRANSLATION_FILE_PATTERN.format(userLocale.toLanguageTag().substringBefore("-")))
                }.getOrNull()
            } ?: run {
                logger.error("Language \"${userLocale.toLanguageTag()}\" is not supported by Katan.")
                logger.error("We will use the fallback language for messages, change the language in the configuration file to one that is supported.")

                userLocale = Locale(FALLBACK_LANGUAGE)
                exportResource(TRANSLATION_FILE_PATTERN.format(userLocale.toLanguageTag()))
            }

            System.setProperty(LOCALE_ENV_PROPERTY, userLocale.toLanguageTag())
            val locale = KatanLocale(userLocale, Properties().apply {
                // force UTF-8 encoding
                BufferedReader(
                    InputStreamReader(
                        FileInputStream(messages),
                        Charsets.UTF_8
                    )
                ).use { input -> load(input) }
            })

            System.setProperty(
                DEBUG_PROPERTY_NAME, when {
                    katanEnv.isDevelopment() || katanEnv.isTesting() -> DEBUG_PROPERTY_VALUE_ON
                    katanEnv.isProduction() -> DEBUG_PROPERTY_VALUE_OFF
                    else -> DEBUG_PROPERTY_VALUE_AUTO
                }
            )
            KatanLauncher(config, katanEnv, locale)
        }

    }

    init {
        val katan = KatanCore(config, environment, locale)
        val cli = KatanCLI(katan)
        val webServer = KatanWS(katan)

        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                cli.close()
                webServer.close()
                katan.close()
            }
        })

        runBlocking {
            try {
                val time = measureTimeMillis {
                    katan.start()
                }

                logger.info(katan.locale["katan.started", String.format("%.2f", time / 1000.0f)])

                if (webServer.enabled)
                    webServer.init()

                cli.init()
            } catch (e: Throwable) {
                when (e) {
                    is SilentException -> {
                        logger.error("An error occurred while starting Katan @ ${e.logger.name.substringAfterLast(".")}:")
                        (e.cause?.message ?: e.message)?.let {
                            logger.error("Cause: \"$it\"")
                        }
                        logger.trace(null, e)

                        if (e.exit)
                            exitProcess(0)
                    }
                    else -> e.printStackTrace()
                }
            }
        }
    }

}
