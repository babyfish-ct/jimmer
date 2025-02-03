package testpkg.configurations;

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.model.filter.File;

public class FileRecursionStrategy implements RecursionStrategy<File> {

    @Override
    public boolean isRecursive(Args<File> args) {
        return args.getEntity().name().equals("etc");
    }
}
