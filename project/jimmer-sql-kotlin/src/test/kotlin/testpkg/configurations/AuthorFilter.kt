package testpkg.configurations

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilter
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.firstName
import org.babyfish.jimmer.sql.kt.model.classic.author.lastName

class AuthorFilter : KFieldFilter<Author> {

    override fun KFieldFilterDsl<Author>.applyTo() {
        where(table.firstName ne "Alex")
        where(table.lastName ne "Banks")
        orderBy(table.firstName.asc(), table.lastName.desc())
    }
}