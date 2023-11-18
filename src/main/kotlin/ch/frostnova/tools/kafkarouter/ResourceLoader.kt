package ch.frostnova.tools.kafkarouter

import ch.frostnova.tools.kafkarouter.config.KafkaRouterConfig
import ch.frostnova.tools.kafkarouter.util.ObjectMappers
import ch.frostnova.tools.kafkarouter.util.logger
import java.io.File

const val configPathEnvVariable = "CONFIG_FILE"
const val configPathDefault = "/config/config.yaml"

object ResourceLoader {

    private val logger = logger(ResourceLoader::class)

    private val configFile: File by lazy {
        val configPath = sequenceOf(
            System.getProperty(configPathEnvVariable),
            System.getenv(configPathEnvVariable)
        ).filterNotNull().firstOrNull() ?: configPathDefault
        assertReadable(File(configPath))
    }

    private val resourceFile: File by lazy {
        configFile.parentFile
    }

    fun loadConfig(): KafkaRouterConfig {
        logger.info("reading configuration from {}", configFile)
        return ObjectMappers.forResource(configFile.name).readValue(configFile, KafkaRouterConfig::class.java)
    }

    fun getResource(name: String): File = assertReadable(File(resourceFile, name))

    private fun assertReadable(file: File): File {
        require(file.exists()) { "File $file not found" }
        require(file.isFile) { "$file is not a file" }
        require(file.canRead()) { "File $file cannot be read" }
        return file
    }
}