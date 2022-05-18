package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class FetcherContext {

    private SqlClient sqlClient;

    private Connection con;

    private DataCache cache = new DataCache();

    private Map<Field, FetcherTask> taskMap = new LinkedHashMap<>();

    public FetcherContext(SqlClient sqlClient, Connection con) {
        this.sqlClient = sqlClient;
        this.con = con;
    }

    public void add(Fetcher<?> fetcher, DraftSpi draft) {
        for (Field field : fetcher.getFieldMap().values()) {
            if (!field.isSimpleField()) {
                FetcherTask task = taskMap.computeIfAbsent(field, it ->
                        new FetcherTask(
                                cache,
                                sqlClient,
                                con,
                                field
                        )
                );
                task.add(draft);
            }
        }
    }

    public void addAll(Fetcher<?> fetcher, Collection<DraftSpi> drafts) {
        for (DraftSpi draft : drafts) {
            add(fetcher, draft);
        }
    }

    public void execute() {
        while (!taskMap.isEmpty()) {
            Iterator<FetcherTask> itr = taskMap.values().iterator();
            FetcherTask task = itr.next();
            if (task.execute()) {
                itr.remove();
            }
        }
    }
}
