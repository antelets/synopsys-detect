/**
 * synopsys-detect
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
package com.synopsys.integration.detect.battery.detector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detect.battery.util.DetectorBatteryTestRunner;
import com.synopsys.integration.detect.configuration.DetectProperties;

@Tag("battery")
public class GradleBattery {
    private static final String RESOURCE_FOLDER = "GRADLE-0";

    @Test
    void gradleFromProperty() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("gradle-property", "gradle-inspector");
        test.executableThatCopiesFiles(DetectProperties.DETECT_GRADLE_PATH.getProperty(), RESOURCE_FOLDER)
            .onWindows(5, "")
            .onLinux(3, "-DGRADLEEXTRACTIONDIR=");
        test.sourceDirectoryNamed("linux-gradle");
        test.sourceFileNamed("build.gradle");
        test.git("https://github.com/BlackDuckCoPilot/example-gradle-travis", "master");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void gradleWrapperFromSourceFile() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("gradle-wrapper", "gradle-inspector");
        test.executableSourceFileThatCopiesFiles("gradlew.bat", "gradlew", RESOURCE_FOLDER)
            .onWindows(5, "")
            .onLinux(3, "-DGRADLEEXTRACTIONDIR=");
        test.sourceDirectoryNamed("linux-gradle");
        test.sourceFileNamed("build.gradle");
        test.git("https://github.com/BlackDuckCoPilot/example-gradle-travis", "master");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void gradleWrapperOnDetect() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("gradle-detect-on-detect", "gradle-detect-on-detect");
        test.executableSourceFileThatCopiesFiles("gradlew.bat", "gradlew", RESOURCE_FOLDER)
            .onWindows(5, "")
            .onLinux(3, "-DGRADLEEXTRACTIONDIR=");
        test.sourceDirectoryNamed("synopsys-detect");
        test.sourceFileNamed("build.gradle");
        test.git("https://github.com/blackducksoftware/synopsys-detect", "master");
        test.expectBdioResources();
        test.run();
    }
}

