package org.babyfish.jimmer.sql.example.business.interceptor.input;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.model.common.BaseEntityDraft;
import org.babyfish.jimmer.sql.example.model.common.BaseEntityProps;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BaseEntityDraftInterceptor implements DraftInterceptor<BaseEntityDraft> {

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

    @Override
    public void beforeSave(BaseEntityDraft draft, boolean isNew) {
        if (!ImmutableObjects.isLoaded(draft, BaseEntityProps.MODIFIED_TIME)) {
            draft.setModifiedTime(LocalDateTime.now());
        }
        if (isNew && !ImmutableObjects.isLoaded(draft, BaseEntityProps.CREATED_TIME)) {
            draft.setCreatedTime(LocalDateTime.now());
        }
    }
}
