package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.dto.compiler.DtoFile;
import org.babyfish.jimmer.dto.compiler.DtoUtils;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class DtoContext {

    private final Filer filer;

    private final List<DtoFile> dtoFiles;

    public DtoContext(Filer filer, Collection<String> dtoDirs) {
        this.filer = filer;
        Map<String, File> dtoDirFileMap = getDtoDirFileMap(dtoDirs);
        List<DtoFile> dtoFiles = new ArrayList<>();
        for (Map.Entry<String, File> e : dtoDirFileMap.entrySet()) {
            File[] subFiles = e.getValue().listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles){
                    collectDtoFiles(e.getKey(), subFile, new ArrayList<>(), dtoFiles);
                }
            }
        }
        this.dtoFiles = dtoFiles;
    }

    public List<DtoFile> getDtoFiles() {
        return Collections.unmodifiableList(dtoFiles);
    }

    private Map<String, File> getDtoDirFileMap(Collection<String> dtoDirs) {
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
        while (baseFile != null) {
            collectDtoDirFiles(baseFile, dtoDirs, dtoDirFileMap);
            baseFile = baseFile.getParentFile();
        }
        return dtoDirFileMap;
    }

    private static void collectDtoDirFiles(File baseFile, Collection<String> dtoDirs, Map<String, File> dtoDirFileMap) {
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
            }
        }
    }

    private static void collectDtoFiles(String dtoDir, File file, List<String> paths, List<DtoFile> dtoFiles) {

        if (file.isFile() && file.getName().endsWith(".dto")) {
            dtoFiles.add(new DtoFile(dtoDir, paths, file));
        } else {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                paths.add(file.getName());
                for (File subFile : subFiles) {
                    collectDtoFiles(dtoDir, subFile, paths, dtoFiles);
                }
                paths.remove(paths.size() - 1);
            }
        }
    }
}
