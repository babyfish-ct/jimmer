package org.babyfish.jimmer.sql.example.graphql.bll.interceptor.input;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntityDraft;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntityProps;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * see JSqlClient.Builder.addDraftInterceptors
 */
@Component
public class BaseEntityDraftInterceptor implements DraftInterceptor<BaseEntityDraft> {

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
