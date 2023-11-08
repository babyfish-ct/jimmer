package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile
import org.babyfish.jimmer.dto.compiler.DtoFile
import java.io.File

class DtoContext {

    val dtoFiles: List<DtoFile>

    constructor(anyFile: KSFile?, dtoDirs: Collection<String>) {
        var file: File? = anyFile?.let { File(it.filePath) }

        val dtoDirFileMap = mutableMapOf<String, File>()
        while (file != null) {
            collectActualDtoDir(file, dtoDirs, dtoDirFileMap)
            file = file.parentFile
        }

        val dtoFiles = mutableListOf<DtoFile>()
        for ((key, value) in dtoDirFileMap) {
            val subFiles = value.listFiles()
            if (subFiles != null) {
                for (subFile in subFiles) {
                    collectDtoFiles(key, subFile, mutableListOf(), dtoFiles)
                }
            }
        }

        this.dtoFiles = dtoFiles
    }

    private fun collectActualDtoDir(baseFile: File, dtoDirs: Collection<String>, dtoDirFileMap: MutableMap<String, File>) {
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
            }
        }
    }

    private fun collectDtoFiles(dtoDir: String, file: File, paths: MutableList<String>, dtoFiles: MutableList<DtoFile>) {
        if (file.isFile() && file.getName().endsWith(".dto")) {
            dtoFiles += DtoFile(dtoDir, paths, file)
        } else {
            val subFiles = file.listFiles()
            if (subFiles != null) {
                paths += file.getName()
                for (subFile in subFiles) {
                    collectDtoFiles(dtoDir, subFile, paths, dtoFiles)
                }
                paths.removeAt(paths.size - 1)
            }
        }
    }
}