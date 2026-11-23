package org.babyfish.jimmer.nojackson;

import org.babyfish.jimmer.client.meta.Api;

@Api
public interface NoteApi {

    @Api
    Note note(long id);
}
