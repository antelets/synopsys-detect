package com.synopsys.integration.configuration.config

import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource

class SpringPropertySource(private val propertySource: org.springframework.core.env.PropertySource<*>) : PropertySource {
    companion object {
        fun fromConfigurableEnvironment(configurableEnvironment: ConfigurableEnvironment): List<SpringPropertySource> {
            return configurableEnvironment.propertySources.map { SpringPropertySource(it) }
        }
    }

    override fun hasKey(key: String): Boolean {
        return propertySource.containsProperty(key)
    }

    override fun getValue(key: String): String? {
        return propertySource.getProperty(key)?.toString()
    }

    override fun getName(): String {
        return propertySource.name
    }

    //A basic 'Spring Property Source' does not have the concept of Origin. Only Spring Configuration Property Sources do.
    override fun getOrigin(key: String): String? {
        return getName();
    }

    override fun getKeys(): Set<String> {
        return if (propertySource is EnumerablePropertySource<*>) {
            propertySource.propertyNames.toSet()
        } else {
            emptySet()
        }
    }

}