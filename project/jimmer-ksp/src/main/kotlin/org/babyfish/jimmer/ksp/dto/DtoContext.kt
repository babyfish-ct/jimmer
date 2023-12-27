package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.symbol.KSFile
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.OsFile
import java.io.File

class DtoContext(anyFile: KSFile?, dtoDirs: Collection<String>) {

    val dtoFiles: List<DtoFile>

    init {
        var file: File? = anyFile?.let { File(it.filePath) }
        val dtoDirFileMap = mutableMapOf<String, File>()
        var projectDir: String? = null
        while (file != null) {
            val prjDir = collectDtoDirFileMap(file, dtoDirs, dtoDirFileMap)
            if (projectDir === null) {
                projectDir = prjDir
            }
            file = file.parentFile
        }
        val dtoFiles = mutableListOf<DtoFile>()
        for ((key, value) in dtoDirFileMap) {
            val subFiles = value.listFiles()
            if (subFiles != null) {
                for (subFile in subFiles) {
                    collectDtoFiles(projectDir!!, key, subFile, mutableListOf(), dtoFiles)
                }
            }
        }
        this.dtoFiles = dtoFiles
    }

    private fun collectDtoDirFileMap(
        baseFile: File,
        dtoDirs: Collection<String>,
        dtoDirFileMap: MutableMap<String, File>
    ) : String? {
        var projectDir: String? = null
        for (dtoDir in dtoDirs) {
            var subFile: File? = baseFile
            for (part in dtoDir.split("/").toTypedArray()) {
                subFile = File(subFile, part)
                if (!subFile.isDirectory) {
                    subFile = null
                    break
                }
            }
            if (subFile != null) {
                dtoDirFileMap[dtoDir] = subFile
                projectDir = baseFile.name
            }
        }
        return projectDir
    }

    private fun collectDtoFiles(projectDir: String, dtoDir: String, file: File, paths: MutableList<String>, dtoFiles: MutableList<DtoFile>) {
        if (file.isFile() && file.getName().endsWith(".dto")) {
            dtoFiles += DtoFile(OsFile.of(file), projectDir, dtoDir, paths, file.name)
        } else {
            val subFiles = file.listFiles()
            if (subFiles != null) {
                paths += file.getName()
                for (subFile in subFiles) {
                    collectDtoFiles(projectDir, dtoDir, subFile, paths, dtoFiles)
                }
                paths.removeAt(paths.size - 1)
            }
        }
    }
}