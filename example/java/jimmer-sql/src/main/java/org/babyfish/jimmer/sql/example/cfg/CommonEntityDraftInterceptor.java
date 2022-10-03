package org.babyfish.jimmer.sql.example.cfg;

import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.example.model.CommonEntityDraft;
import org.babyfish.jimmer.sql.example.model.CommonEntityProps;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommonEntityDraftInterceptor implements DraftInterceptor<CommonEntityDraft> {

    @Override
    public void beforeSave(CommonEntityDraft draft, boolean isNew) {
        if (!ImmutableObjects.isLoaded(draft, CommonEntityProps.MODIFIED_TIME)) {
            draft.setModifiedTime(LocalDateTime.now());
        }
        if (isNew && !ImmutableObjects.isLoaded(draft, CommonEntityProps.CREATED_TIME)) {
            draft.setCreatedTime(LocalDateTime.now());
        }
    }
}
