/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.configuration.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.configuration.config.PropertyConfiguration;
import com.synopsys.integration.configuration.help.PropertyConfigurationHelpContext;
import com.synopsys.integration.configuration.property.Property;
import com.synopsys.integration.configuration.property.PropertyDeprecationInfo;
import com.synopsys.integration.detect.configuration.DetectInfo;
import com.synopsys.integration.detect.configuration.DetectProperties;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.status.DetectIssue;
import com.synopsys.integration.detect.workflow.status.DetectIssueType;

public class DetectConfigurationBootManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventSystem eventSystem;
    private final DetectInfo detectInfo;
    private final PropertyConfigurationHelpContext detectConfigurationReporter;

    public DetectConfigurationBootManager(EventSystem eventSystem, DetectInfo detectInfo, PropertyConfigurationHelpContext detectConfigurationReporter) {
        this.eventSystem = eventSystem;
        this.detectInfo = detectInfo;
        this.detectConfigurationReporter = detectConfigurationReporter;
    }

    public DeprecationResult checkForDeprecations(PropertyConfiguration detectConfiguration) throws IllegalAccessException {
        Map<String, String> additionalNotes = new HashMap<>();

        List<Property> usedDeprecatedProperties = DetectProperties.allProperties().getProperties()
                                                      .stream()
                                                      .filter(property -> detectConfiguration.wasKeyProvided(property.getKey()))
                                                      .filter(property -> property.getPropertyDeprecationInfo() != null)
                                                      .collect(Collectors.toList());

        for (Property property : usedDeprecatedProperties) {
            PropertyDeprecationInfo deprecationInfo = property.getPropertyDeprecationInfo();
            String deprecationMessage = deprecationInfo.getDeprecationText();
            String propertyKey = property.getKey();

            additionalNotes.put(propertyKey, "\t *** DEPRECATED ***");

            DetectIssue.publish(eventSystem, DetectIssueType.DEPRECATION, propertyKey, deprecationMessage);
        }

        return new DeprecationResult(additionalNotes);
    }

    public void printConfiguration(SortedMap<String, String> maskedRawPropertyValues, Set<String> propertyKeys, Map<String, String> additionalNotes) throws IllegalAccessException {
        detectConfigurationReporter.printKnownCurrentValues(logger::info, propertyKeys, maskedRawPropertyValues, additionalNotes);
    }

    //Check for options that are just plain bad, ie giving an detector type we don't know about.
    public Optional<DetectUserFriendlyException> validateForPropertyParseErrors() throws IllegalAccessException {
        Map<String, List<String>> errorMap = detectConfigurationReporter.findPropertyParseErrors(DetectProperties.allProperties().getProperties());
        if (!errorMap.isEmpty()) {
            Map.Entry<String, List<String>> entry = errorMap.entrySet().iterator().next();
            return Optional.of(new DetectUserFriendlyException(entry.getKey() + ": " + entry.getValue().get(0), ExitCodeType.FAILURE_GENERAL_ERROR));
        }
        return Optional.empty();
    }

    public void printFailingPropertiesMessages(Map<String, List<String>> deprecationMessages) throws IllegalAccessException {
        Set<String> sortedPropertyKeys = new HashSet(DetectProperties.allProperties().getPropertyKeys());
        detectConfigurationReporter.printKnownPropertyErrors(logger::info, sortedPropertyKeys, new TreeMap(deprecationMessages));

        logger.warn(StringUtils.repeat("=", 60));
        logger.warn("Configuration is using deprecated properties that must be updated for this major version.");
        logger.warn("You MUST fix these deprecation issues for detect to proceed.");
        logger.warn(String.format("To ignore these messages and force detect to exit with success supply --%s=true", DetectProperties.DETECT_FORCE_SUCCESS.getProperty().getKey()));
        logger.warn("This will not force detect to run, but it will pretend to have succeeded.");
        logger.warn(StringUtils.repeat("=", 60));

        eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_CONFIGURATION));
    }

}
