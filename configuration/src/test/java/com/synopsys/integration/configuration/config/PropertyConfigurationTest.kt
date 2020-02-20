/**
 * configuration
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.configuration.config

import com.synopsys.integration.configuration.parse.ValueParseException
import com.synopsys.integration.configuration.parse.ValueParser
import com.synopsys.integration.configuration.property.base.NullableProperty
import com.synopsys.integration.configuration.property.base.PassthroughProperty
import com.synopsys.integration.configuration.property.base.ValuedProperty
import com.synopsys.integration.configuration.source.MapPropertySource
import com.synopsys.integration.configuration.util.ConfigTestUtils.configOf
import com.synopsys.integration.configuration.util.ConfigTestUtils.emptyConfig
import com.synopsys.integration.configuration.util.ConfigTestUtils.propertySourceOf
import org.apache.commons.lang3.tuple.Pair
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class PropertyConfigurationTest {
    companion object {
        const val UNKNOWN_VALUE = "-1"
    }

    class TestValueParser : ValueParser<String>() {
        override fun parse(value: String): String {
            if (value == UNKNOWN_VALUE) {
                throw ValueParseException(value, "String", "Will parse any value to String, except for '-1' for the test.")
            }
            return value
        }
    }

    class NullableTestProperty(key: String) : NullableProperty<String>(key, TestValueParser())
    class ValuedTestProperty(key: String, default: String) : ValuedProperty<String>(key, TestValueParser(), default)

    //#region Recommended Usage

    @Test
    fun getValueOrNull() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertEquals(Optional.empty<String>(), configOf(Pair.of(nullableProperty.key, UNKNOWN_VALUE)).getValueOrEmpty(nullableProperty), "An unknown value should fail to parse and the config should provide null.")
    }

    @Test
    fun getValueOrDefault() {
        val propertyWithDefault = ValuedTestProperty("example.key", "defaultValue")
        Assertions.assertEquals(propertyWithDefault.defaultValue, configOf(Pair.of(propertyWithDefault.key, UNKNOWN_VALUE)).getValueOrDefault(propertyWithDefault), "An unknown value should fail to parse and the config should provide the default value.")
    }

    @Test
    fun getValueNullableValue() {
        val nullableProperty = NullableTestProperty("example.key")
        val config = configOf(Pair.of(nullableProperty.key, "providedValue"))
        Assertions.assertEquals(Optional.of("providedValue"), config.getValue(nullableProperty), "A provided nullable property should return the provided value.")
    }

    @Test
    fun getValueThrowsOnParseFailureNullable() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertThrows(InvalidPropertyException::class.java, {
            configOf(Pair.of(nullableProperty.key, UNKNOWN_VALUE)).getValue(nullableProperty)
        }, "Should throw an exception when failing to parse.")
    }

    @Test
    fun getValueNullableNull() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertEquals(Optional.empty<String>(), emptyConfig().getValue(nullableProperty), "Config should provide an empty Optional if the property is nullable.")
    }

    @Test
    fun getValueOverridesDefault() {
        val propertyWithDefault = ValuedTestProperty("example.key", "defaultValue")
        val config = configOf(Pair.of(propertyWithDefault.key, "overridden"))
        Assertions.assertEquals("overridden", config.getValue(propertyWithDefault), "A valid provided value should override any default value.")
    }


    @Test()
    fun getValueThrowsOnParseFailureTyped() {
        val propertyWithDefault = ValuedTestProperty("example.key", "defaultValue")
        Assertions.assertThrows(InvalidPropertyException::class.java, {
            configOf(Pair.of(propertyWithDefault.key, UNKNOWN_VALUE)).getValue(propertyWithDefault)
        }, "Should throw an exception when failing to parse.")
    }

    @Test
    fun getValueProvidesDefault() {
        val propertyWithDefault = ValuedTestProperty("example.key", "defaultValue")
        Assertions.assertEquals(propertyWithDefault.defaultValue, emptyConfig().getValue(propertyWithDefault), "Config should provide default value when property is not provided.")
    }

    @Test()
    fun wasKeyProvided() {
        val exampleKey = "example.key"
        Assertions.assertTrue(configOf(Pair.of(exampleKey, UNKNOWN_VALUE)).wasKeyProvided(exampleKey), "The key was provided.")
        Assertions.assertFalse(emptyConfig().wasKeyProvided(exampleKey), "The key was not provided.")
    }

    @Test()
    fun wasPropertyProvided() {
        val propertyWithDefault = ValuedTestProperty("example.key", "defaultValue")
        Assertions.assertTrue(configOf(Pair.of(propertyWithDefault.key, UNKNOWN_VALUE)).wasPropertyProvided(propertyWithDefault), "The property was provided.")
        Assertions.assertFalse(emptyConfig().wasPropertyProvided(propertyWithDefault), "The property was not provided.")
    }

    @Test
    fun getPropertySource() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertEquals(Optional.of("map"), configOf(Pair.of(nullableProperty.key, UNKNOWN_VALUE)).getPropertySource(nullableProperty), "The source of this property should exist.")
        Assertions.assertEquals(Optional.empty<String>(), emptyConfig().getPropertySource(nullableProperty), "The property is not provided and therefore should not have a source.")
    }

    @Test
    fun getPropertyOrigin() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertEquals(Optional.of("map"), configOf(Pair.of(nullableProperty.key, UNKNOWN_VALUE)).getPropertyOrigin(nullableProperty), "The property was provided and should have an origin.")
        Assertions.assertEquals(Optional.empty<String>(), emptyConfig().getPropertyOrigin(nullableProperty), "The property was not provided and should not have an origin.")
    }

    @Test
    fun getKeys() {
        Assertions.assertEquals(setOf("example.key", "other.key"), configOf(Pair.of("example.key", UNKNOWN_VALUE), Pair.of("other.key", UNKNOWN_VALUE)).getKeys(), "The set of keys returned is not identical to all those provided.")
        Assertions.assertEquals(emptySet<String>(), emptyConfig().getKeys(), "The property was not provided and should not have an origin.")
    }

    @Test
    fun getPropertyException() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertTrue(configOf(Pair.of(nullableProperty.key, UNKNOWN_VALUE)).getPropertyException(nullableProperty).isPresent, "The property value should not parse successfully.")
        Assertions.assertEquals(Optional.empty<String>(), configOf(Pair.of(nullableProperty.key, "something")).getPropertyException(nullableProperty), "The property was provided and should be parsable.")
        Assertions.assertEquals(Optional.empty<ValueParseException>(), emptyConfig().getPropertyException(nullableProperty), "The property was not provided and should not have an exception value.")
    }

    //#endregion Recommended Usage

    //region Advanced Usage

    @Test
    fun getRawFromProperty() {
        val nullableProperty = NullableTestProperty("example.key")
        Assertions.assertEquals(Optional.of(" true "), configOf(Pair.of(nullableProperty.key, " true ")).getRaw(nullableProperty), "The property should be resolved.")
        Assertions.assertEquals(Optional.empty<String>(), emptyConfig().getRaw(nullableProperty), "The property was not provided and should not resolve a value.")
    }

    @Test
    fun getRaw() {
        val nullableProperty = NullableTestProperty("example.key")
        val valuedProperty = ValuedTestProperty("property.two.key", "test")
        val propertyMap = mapOf(
                nullableProperty.key to " true ",
                valuedProperty.key to ""
        )
        val config = configOf(MapPropertySource("map", propertyMap))
        Assertions.assertEquals(propertyMap, config.getRaw(), "The map provided by the config should match the property source it was given.")
        Assertions.assertEquals(emptyMap<String, String>(), emptyConfig().getRaw(), "The config should not have any values to provide.")
    }

    @Test
    fun getRawFromKeys() {
        val nullableProperty = NullableTestProperty("example.key")
        val valuedProperty = ValuedTestProperty("property.two.key", "test")
        val propertyMap = mapOf(
                nullableProperty.key to " true ",
                valuedProperty.key to ""
        )
        val config = configOf(MapPropertySource("map", propertyMap))

        Assertions.assertEquals(propertyMap, config.getRaw(propertyMap.keys), "All keys should match.")

        val extraKeys = setOf("unrelated.key", *propertyMap.keys.toTypedArray())
        Assertions.assertEquals(propertyMap, config.getRaw(extraKeys), "Expected entries should not include any unrelated keys.")

        Assertions.assertEquals(emptyMap<String, String>(), config.getRaw(setOf("unrelated.key")), "The config should not provide any values.")
        Assertions.assertEquals(emptyMap<String, String>(), config.getRaw(setOf()), "The config should not provide any values.")
    }

    @Test
    fun getRawFromPredicate() {
        val nullableProperty = NullableTestProperty("example.key")
        val valuedProperty = ValuedTestProperty("property.two.key", "test")
        val propertyMap = mapOf(
                nullableProperty.key to " true ",
                valuedProperty.key to ""
        )
        val config = configOf(MapPropertySource("map", propertyMap))

        Assertions.assertEquals(propertyMap, config.getRaw { true }, "All keys should match.")

        Assertions.assertEquals(1, config.getRaw { propertyKey -> propertyKey == nullableProperty.key }.size, "Expected entries should not include any unrelated keys.")

        Assertions.assertEquals(emptyMap<String, String>(), config.getRaw { false }, "The config should not provide any values to provide.")
    }

    @Test
    fun getRawPassthroughMutlipleValues() {
        val passthrough = PassthroughProperty("pass")
        val secondarySource = propertySourceOf("secondary", Pair.of("pass.two", "two value"), Pair.of("ignore", "ignore value"))
        val primarySource = propertySourceOf("primary", Pair.of("pass.one", "one value"))
        val configuration = configOf(primarySource, secondarySource)

        Assertions.assertEquals(mapOf("one" to "one value", "two" to "two value"), configuration.getRaw(passthrough))
    }

    @Test
    fun getRawPassthroughPrimary() {
        val passthrough = PassthroughProperty("pass")
        val secondarySource = propertySourceOf("secondary", Pair.of("pass.shared", "secondaryValue"))
        val primarySource = propertySourceOf("primary", Pair.of("pass.shared", "primaryValue"))
        val configuration = configOf(primarySource, secondarySource)

        Assertions.assertEquals(mapOf("shared" to "primaryValue"), configuration.getRaw(passthrough))
    }

    //endregion Advanced Usage
}