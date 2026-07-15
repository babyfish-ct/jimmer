package org.babyfish.jimmer.sql.model.fetcher;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Issue1434MessageUserDepartmentNamesResolver implements TransientResolver<Long, String> {

    private final JSqlClient sqlClient;

    public Issue1434MessageUserDepartmentNamesResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Map<Long, String> resolve(Collection<Long> ids) {
        Issue1434MessageTable table = Issue1434MessageTable.$;
        List<Issue1434Message> messages = sqlClient
                .createQuery(table)
                .where(table.id().in(ids))
                .select(
                        table.fetch(
                                Issue1434MessageFetcher.$.user(
                                        Issue1434UserFetcher.$.departments(
                                                Issue1434DepartmentFetcher.$.name()
                                        )
                                )
                        )
                )
                .execute(TransientResolver.currentConnection());
        Map<Long, String> map = new LinkedHashMap<>((messages.size() * 4 + 2) / 3);
        for (Issue1434Message message : messages) {
            Issue1434User user = message.user();
            map.put(
                    message.id(),
                    user != null ?
                            user
                                    .departments()
                                    .stream()
                                    .map(Issue1434Department::name)
                                    .collect(Collectors.joining(",")) :
                            ""
            );
        }
        return map;
    }
}
