package com.synopsys.integration.detect.battery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;

import freemarker.template.TemplateException;

public class ResourceCopyingExecutableCreator extends BatteryExecutableCreator {
    private final List<String> toCopy;
    private OperatingSystemInfo windowsInfo = null;
    private OperatingSystemInfo linuxInfo = null;

    private static class OperatingSystemInfo {
        final int extractionFolderIndex;
        final String extractionFolderPrefix;

        private OperatingSystemInfo(final int extractionFolderIndex, final String extractionFolderPrefix) {
            this.extractionFolderIndex = extractionFolderIndex;
            this.extractionFolderPrefix = extractionFolderPrefix;
        }
    }

    ResourceCopyingExecutableCreator(final List<String> toCopy) {
        this.toCopy = toCopy;
    }

    public ResourceCopyingExecutableCreator onAnySystem(final int extractionFolderIndex, final String extractionFolderPrefix) {
        return onWindows(extractionFolderIndex, extractionFolderPrefix).onLinux(extractionFolderIndex, extractionFolderPrefix);
    }

    public ResourceCopyingExecutableCreator onWindows(final int extractionFolderIndex, final String extractionFolderPrefix) {
        this.windowsInfo = new OperatingSystemInfo(extractionFolderIndex, extractionFolderPrefix);
        return this;
    }

    public ResourceCopyingExecutableCreator onLinux(final int extractionFolderIndex, final String extractionFolderPrefix) {
        this.linuxInfo = new OperatingSystemInfo(extractionFolderIndex, extractionFolderPrefix);
        return this;
    }

    //Map of Names to Data file paths.
    private Map<String, String> getFilePaths(final File mockDirectory, final AtomicInteger commandCount) throws IOException {
        final Map<String, String> filePaths = new HashMap<>();
        for (final String resource : toCopy) {
            final File copyingFolder = BatteryFiles.asFile(resource);
            final File[] files = copyingFolder.listFiles();
            Assertions.assertNotNull(files, "When a resource copying executable is used, it should be provided a resource folder. Verify it is a folder and has at least one file: " + resource);
            for (final File file : files) {
                final File commandTextFile = new File(mockDirectory, "data-" + commandCount.getAndIncrement() + ".dat");
                Files.copy(file.toPath(), commandTextFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                filePaths.put(commandTextFile.getCanonicalPath(), file.getName());
            }
        }
        return filePaths;
    }

    @Override
    public File createExecutable(final int id, final File mockDirectory, final AtomicInteger commandCount) throws IOException, TemplateException {
        final Map<String, Object> model = new HashMap<>();
        Assertions.assertNotNull(linuxInfo, "If you have a resource copying executable, you must specify operating system information for both windows and linux but linux information could not be found.");
        Assertions.assertNotNull(windowsInfo, "If you have a resource copying executable, you must specify operating system information for both windows and linux but windows information could not be found.");
        if (SystemUtils.IS_OS_WINDOWS) {
            model.put("extractionFolderIndex", windowsInfo.extractionFolderIndex);
            model.put("extractionFolderPrefix", windowsInfo.extractionFolderPrefix);
        } else {
            model.put("extractionFolderIndex", linuxInfo.extractionFolderIndex);
            model.put("extractionFolderPrefix", linuxInfo.extractionFolderPrefix);
        }
        final List<Object> files = new ArrayList<>();
        getFilePaths(mockDirectory, commandCount).forEach((key, value) -> {
            final Map<String, String> modelEntry = new HashMap<>();
            modelEntry.put("from", key);
            modelEntry.put("to", value);
            files.add(modelEntry);
        });
        model.put("files", files);

        final File commandFile;
        if (SystemUtils.IS_OS_WINDOWS) {
            commandFile = new File(mockDirectory, "exe-" + id + ".bat");
            BatteryFiles.processTemplate("/copying-exe.ftl", commandFile, model, BatteryFiles.UTIL_RESOURCE_PREFIX);
        } else {
            commandFile = new File(mockDirectory, "sh-" + id + ".sh");
            BatteryFiles.processTemplate("/copying-sh.ftl", commandFile, model, BatteryFiles.UTIL_RESOURCE_PREFIX);
            Assertions.assertTrue(commandFile.setExecutable(true));
        }

        return commandFile;
    }
}
