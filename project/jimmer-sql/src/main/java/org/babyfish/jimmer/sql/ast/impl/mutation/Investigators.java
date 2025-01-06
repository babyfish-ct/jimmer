package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

class Investigators {

    private Investigators() {}

    public static JSqlClientImplementor toInvestigatorSqlClient(
            JSqlClientImplementor sqlClient,
            ExceptionTranslator.Args args
    ) {
        ExecutorForLog executorLog = AbstractExecutorProxy.as(
                sqlClient.getExecutor(),
                ExecutorForLog.class
        );
        if (executorLog == null || executorLog.getLogger() instanceof InvestigatorLogger) {
            return sqlClient;
        }
        InvestigatorLogger investigatorLogger = new InvestigatorLogger(executorLog.getLogger());
        if (args instanceof Executor.BatchContext) {
            ((Executor.BatchContext)args).addExecutedListener(investigatorLogger::submit);
        }
        return sqlClient.executor(
                ExecutorForLog.wrap(sqlClient.getExecutor(), investigatorLogger)
        );
    }
}
