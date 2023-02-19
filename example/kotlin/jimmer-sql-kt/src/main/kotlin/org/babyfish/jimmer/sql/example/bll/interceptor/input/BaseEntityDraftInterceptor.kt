package org.babyfish.jimmer.sql.example.bll.interceptor.input

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.model.common.BaseEntity
import org.babyfish.jimmer.sql.example.model.common.BaseEntityDraft
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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