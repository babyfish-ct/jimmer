package org.babyfish.jimmer.example.kt.graphql.interceptor.input

import org.babyfish.jimmer.example.kt.graphql.entities.common.CommonEntity
import org.babyfish.jimmer.example.kt.graphql.entities.common.CommonEntityDraft
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/*
 * see KSqlClientDsl.addDraftInterceptors
 */
@Component
class CommonEntityDraftInterceptor : DraftInterceptor<CommonEntityDraft> {

    override fun beforeSave(draft: CommonEntityDraft, isNew: Boolean) {
        if (!isLoaded(draft, CommonEntity::modifiedTime)) {
            draft.modifiedTime = LocalDateTime.now()
        }
        if (isNew && !isLoaded(draft, CommonEntity::createdTime)) {
            draft.createdTime = LocalDateTime.now()
        }
    }
}