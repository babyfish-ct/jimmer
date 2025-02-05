package testpkg.configurations

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy
import org.babyfish.jimmer.sql.kt.model.filter.File

class FileRecursionStrategy : RecursionStrategy<File> {
    override fun isRecursive(args: RecursionStrategy.Args<File>): Boolean {
        return args.entity.name == "etc"
    }
}
