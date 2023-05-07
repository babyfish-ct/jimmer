package org.babyfish.jimmer.sql.example.business.interceptor.input

import org.babyfish.jimmer.kt.isLoaded
import org.babyfish.jimmer.sql.DraftInterceptor
import org.babyfish.jimmer.sql.example.model.common.BaseEntity
import org.babyfish.jimmer.sql.example.model.common.BaseEntityDraft
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BaseEntityDraftInterceptor : DraftInterceptor<BaseEntityDraft> {

    /*
     * In this simple example, `BaseEntity` has only two fields: `createdTime` and `modifiedTime`.
     *
     * In actual projects, you can add more fields, such as `creator` and `modifier`,
     * and you can use the information of the permission system to set them as the current user.
     *
     * Since `DraftInterceptor` itself is a spring object, you can use any business information
     * for draft filling. This is why jimmer uses Spring-managed `DraftInterceptor` instead of
     * simply using ORM to support default value.
     */

    override fun beforeSave(draft: BaseEntityDraft, isNew: Boolean) {
        if (!isLoaded(draft, BaseEntity::modifiedTime)) {
            draft.modifiedTime = LocalDateTime.now()
        }
        if (isNew && !isLoaded(draft, BaseEntity::createdTime)) {
            draft.createdTime = LocalDateTime.now()
        }
    }
}