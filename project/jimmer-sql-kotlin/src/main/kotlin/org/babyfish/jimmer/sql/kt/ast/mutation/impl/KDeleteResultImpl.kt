package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.mutation.DeleteResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteResult

internal class KDeleteResultImpl(
    javaResult: DeleteResult
) : KMutationResultImpl(javaResult), KDeleteResult