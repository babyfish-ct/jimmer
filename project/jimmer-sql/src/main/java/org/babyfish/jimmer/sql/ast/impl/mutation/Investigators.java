package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

class Investigators {

    private Investigators() {}

    public static JSqlClientImplementor toInvestigatorSqlClient(
            JSqlClientImplementor sqlClient,
            @Nullable Executor.BatchContext ctx
    ) {
        ExecutorForLog executorLog = AbstractExecutorProxy.as(
                sqlClient.getExecutor(),
                ExecutorForLog.class
        );
        if (executorLog == null || executorLog.getLogger() instanceof InvestigatorLogger) {
            return sqlClient;
        }
        InvestigatorLogger investigatorLogger = new InvestigatorLogger(executorLog.getLogger());
        if (ctx != null) {
            ctx.addExecutedListener(investigatorLogger::submit);
        }
        return sqlClient.executor(
                ExecutorForLog.wrap(sqlClient.getExecutor(), investigatorLogger)
        );
    }
}
