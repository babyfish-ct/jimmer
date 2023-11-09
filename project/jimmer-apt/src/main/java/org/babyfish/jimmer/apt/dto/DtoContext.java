package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.dto.compiler.DtoFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class DtoContext {

    private final Filer filer;

    private final List<DtoFile> dtoFiles;

    public DtoContext(Filer filer, Collection<String> dtoDirs) {
        this.filer = filer;
        DtoDirInfo dtoDirInfo = getDtoDirInfo(dtoDirs);
        List<DtoFile> dtoFiles = new ArrayList<>();
        for (Map.Entry<String, File> e : dtoDirInfo.dtoDirFileMap.entrySet()) {
            File[] subFiles = e.getValue().listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles){
                    collectDtoFiles(dtoDirInfo.projectDir, e.getKey(), subFile, new ArrayList<>(), dtoFiles);
                }
            }
        }
        this.dtoFiles = dtoFiles;
    }

    public List<DtoFile> getDtoFiles() {
        return Collections.unmodifiableList(dtoFiles);
    }

    private DtoDirInfo getDtoDirInfo(Collection<String> dtoDirs) {
        String basePath;
        try {
            basePath = filer.getResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "dummy.txt"
            ).toUri().getPath();
        } catch (IOException ex) {
            throw new DtoException("Failed to guess base project dir", ex);
        }
        if (basePath.startsWith("file:")) {
            basePath = basePath.substring(5);
        }
        if (File.separatorChar != '\\' && !basePath.startsWith("/")) {
            basePath = '/' + basePath;
        }
        basePath = basePath.substring(0, basePath.lastIndexOf('/'));
        File baseFile;
        try {
            baseFile = new File(URLDecoder.decode(basePath, "utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError("UTF-8 is not supported by url decoder");
        }
        if (!baseFile.exists()) {
            throw new AssertionError("The target directory \"" + basePath + "\" does not exists");
        }
        Map<String, File> dtoDirFileMap = new LinkedHashMap<>();
        String projectDir = null;
        while (baseFile != null) {
            String prjDir = collectDtoDirFiles(baseFile, dtoDirs, dtoDirFileMap);
            if (projectDir == null) {
                projectDir = prjDir;
            }
            baseFile = baseFile.getParentFile();
        }
        return new DtoDirInfo(projectDir, dtoDirFileMap);
    }

    private static String collectDtoDirFiles(@NotNull File baseFile, Collection<String> dtoDirs, Map<String, File> dtoDirFileMap) {
        String projectDir = null;
        for (String dtoDir : dtoDirs) {
            File subFile = baseFile;
            for (String part : dtoDir.split("/")) {
                subFile = new File(subFile, part);
                if (!subFile.isDirectory()) {
                    subFile = null;
                    break;
                }
            }
            if (subFile != null) {
                dtoDirFileMap.put(dtoDir, subFile);
                projectDir = baseFile.getName();
            }
        }
        return projectDir;
    }

    private static void collectDtoFiles(String projectDir, String dtoDir, File file, List<String> paths, List<DtoFile> dtoFiles) {
        if (file.isFile() && file.getName().endsWith(".dto")) {
            dtoFiles.add(
                    new DtoFile(projectDir, dtoDir, paths, file.getName(), () ->
                            new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)
                    )
            );
        } else {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                paths.add(file.getName());
                for (File subFile : subFiles) {
                    collectDtoFiles(projectDir, dtoDir, subFile, paths, dtoFiles);
                }
                paths.remove(paths.size() - 1);
            }
        }
    }

    private static class DtoDirInfo {

        final String projectDir;

        final Map<String, File> dtoDirFileMap;

        DtoDirInfo(String projectDir, Map<String, File> dtoDirFileMap) {
            this.projectDir = projectDir;
            this.dtoDirFileMap = dtoDirFileMap;
        }
    }
}
