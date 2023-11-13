package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.example.Context;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.RecursiveFieldConfig;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileService implements Context {

    private static final Pattern SLASH_PATTERN = Pattern.compile("/");

    private static final Fetcher<File> UP_RECURSION =
            FileFetcher.$
                    .allScalarFields()
                    .parent(
                            FileFetcher.$.allScalarFields(),
                            RecursiveFieldConfig::recursive
                    );

    private static final Fetcher<File> DOWN_RECURSION =
            FileFetcher.$
                    .allScalarFields()
                    .subFiles(
                            FileFetcher.$.allScalarFields(),
                            RecursiveListFieldConfig::recursive
                    );

    private static final Fetcher<File> DETAIL_DOWN_RECURSION =
            FileFetcher.$
                    .allScalarFields()
                    .authorizedUsers(
                            UserFetcher.$.allScalarFields()
                    )
                    .subFiles(
                            FileFetcher.$
                                    .allScalarFields()
                                    .authorizedUsers(
                                            UserFetcher.$.allScalarFields()
                                    ),
                            RecursiveListFieldConfig::recursive
                    );

    public File findByPath(String path) {
        List<String> parts = Arrays.stream(SLASH_PATTERN.split(path))
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());
        if (parts.isEmpty()) {
            return null;
        }
        File file = null;
        for (int i = 0; i < parts.size(); i++) {
            File parent = file;
            file = findByParentIdAndName(
                    parent != null ? parent.id() : null,
                    parts.get(i)
            );
            if (file == null) {
                throw new NotExistsException(
                        path,
                        String.join("/", parts.subList(0, i + 1))
                );
            }
        }
        return file;
    }

    public File findByParentIdAndName(@Nullable Long parentId, String name) {
        FileTable table = FileTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .where(table.parentId().eq(parentId)) // In jimmer, `eq(null)` will be consider as `is null`
                .where(table.name().eq(name))
                .select(table)
                .fetchOneOrNull();
    }

    public List<File> findRootDetailTrees() {
        FileTable table = FileTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .where(table.parentId().isNull())
                .orderBy(table.name())
                .select(table.fetch(DETAIL_DOWN_RECURSION))
                .execute();
    }

    public File findSubDetailTree(long id) {
        FileTable table = FileTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .where(table.id().eq(id))
                .select(table.fetch(DETAIL_DOWN_RECURSION))
                .fetchOneOrNull();
    }

    public File findSubTree(long id) {
        FileTable table = FileTable.$;
        return SQL_CLIENT
                .createQuery(table)
                .where(table.id().eq(id))
                .select(table.fetch(DOWN_RECURSION))
                .fetchOneOrNull();
    }

    public void save(File file) {
        long id = SQL_CLIENT.save(file).getModifiedEntity().id();
        SQL_CLIENT
                .getAssociations(FileProps.AUTHORIZED_USERS)
                .saveCommand(id, USER_SERVICE.currentUser().id())
                .checkExistence(true)
                .execute();
    }

    public void delete(long id, boolean recursive) {
        SQL_CLIENT
                .getEntities()
                .deleteCommand(File.class, id)
                .setDissociateAction(
                        FileProps.PARENT,
                        recursive ? DissociateAction.NONE : DissociateAction.DELETE
                )
                .execute();
    }

    public boolean isGranted(long id, long userId) {
        AssociationTable<File, FileTableEx, User, UserTableEx> mappingTable =
                AssociationTable.of(FileTableEx.class, FileTableEx::authorizedUsers);
        return SQL_CLIENT.createAssociationQuery(mappingTable)
                .where(mappingTable.sourceId().eq(id))
                .where(mappingTable.targetId().eq(userId))
                .select(Expression.constant(1))
                .fetchOneOrNull() != null;
    }

    public void grant(long id, long userId) {
        if (!FILE_SERVICE.isGranted(id, USER_SERVICE.currentUser().id())) {
            throw new IllegalArgumentException(
                    "The file does not belong to you so it cannot be granted to other user"
            );
        }

        List<Long>allFileIds = new ArrayList<>();
        File file = SQL_CLIENT.findById(UP_RECURSION, id);
        for (File f = file; f != null; f = f.parent()) {
            allFileIds.add(f.id());
        }

        SQL_CLIENT
                .getAssociations(FileProps.AUTHORIZED_USERS)
                .batchSaveCommand(allFileIds, Collections.singletonList(userId))
                .checkExistence(true)
                .execute();
    }

    public void revoke(long id, long userId) {
        if (userId == USER_SERVICE.currentUser().id()) {
            throw new IllegalArgumentException(
                    "You cannot revoke file from yourself"
            );
        }
        if (!FILE_SERVICE.isGranted(id, USER_SERVICE.currentUser().id())) {
            throw new IllegalArgumentException(
                    "The file does not belong to you so it cannot be revoke from other user"
            );
        }

        List<Long> allFileIds = allFileIds = new ArrayList<>();
        File file = SQL_CLIENT.findById(DOWN_RECURSION, id);
        if (file != null) {
            collectTreeIds(file, allFileIds);
        }

        SQL_CLIENT
                .getAssociations(FileProps.AUTHORIZED_USERS)
                .deleteAll(allFileIds, Collections.singletonList(userId));
    }

    private static void collectTreeIds(File file, Collection<Long> ids) {
        ids.add(file.id());
        for (File subFile : file.subFiles()) {
            collectTreeIds(subFile, ids);
        }
    }
}
