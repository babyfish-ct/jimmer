package org.babyfish.ksp

import org.yaml.snakeyaml.Yaml
import kotlin.io.path.Path

object YmlUtil {
    private val yaml = Yaml()


    fun String?.replaceEnvInString(defaultValueIfNull: String = ""): String {
        this ?: return defaultValueIfNull
        val regex = Regex("\\$\\{(\\w+)(?::([^}]*))?\\}")
        return regex.replace(this) { matchResult ->
            val (envVar, defaultValue) = matchResult.destructured
            val getenv = System.getenv(envVar)
            println("环境变量${envVar}拿到的值为: $getenv, 默认值为: $defaultValue")
            getenv ?: defaultValue
        }
    }


    fun <T> loadYmlConfig(dir: String): T {
        val path = Path(dir)
        val toFile = path.toFile()

        val config = yaml.load<T>(toFile.inputStream())
        return config
    }

    fun loadYmlConfigMap(dir: String): Map<String, Any> {
        val bool = loadYmlConfig<Map<String, Any>>(dir)
        return bool
    }

    fun getActivate(dir: String): String {
        val loadYmlConfigMap = loadYmlConfigMap(dir)
        val configValue = getConfigValue<String>(loadYmlConfigMap, "spring.profiles.active")
        return configValue ?: "local"
    }


    fun <T> getConfigValue(config: Map<String, Any>, path: String): T? {
        val keys = path.split(".")
        var current: Any? = config

        for (key in keys) {
            current = when (current) {
                is Map<*, *> -> current[key]
                else -> return null
            }
        }

        @Suppress("UNCHECKED_CAST")
        return current as? T
    }


}
