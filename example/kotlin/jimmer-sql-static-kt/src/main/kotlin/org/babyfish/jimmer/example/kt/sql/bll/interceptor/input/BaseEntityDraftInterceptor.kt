package org.babyfish.jimmer.example.kt.sql.bll.interceptor.input

import org.babyfish.jimmer.example.kt.sql.model.common.BaseEntity
import org.babyfish.jimmer.example.kt.sql.model.common.BaseEntityDraft
import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/*
 * see KSqlClientDsl.addDraftInterceptors
 */
@Component
class BaseEntityDraftInterceptor : DraftInterceptor<BaseEntityDraft> {

    override fun beforeSave(draft: BaseEntityDraft, isNew: Boolean) {
        if (!isLoaded(draft, BaseEntity::modifiedTime)) {
            draft.modifiedTime = LocalDateTime.now()
        }
        if (isNew && !isLoaded(draft, BaseEntity::createdTime)) {
            draft.createdTime = LocalDateTime.now()
        }
    }
}