package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.runtime.AbstractExecutorProxy;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ExecutorForLog;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

class Investigators {

    private Investigators() {}

    public static JSqlClientImplementor toInvestigatorSqlClient(
            JSqlClientImplementor sqlClient,
            Executor.BatchContext ctx
    ) {
        ExecutorForLog executorLog = AbstractExecutorProxy.as(
                sqlClient.getExecutor(),
                ExecutorForLog.class
        );
        if (executorLog == null || executorLog.getLogger() instanceof InvestigatorLogger) {
            return sqlClient;
        }
        InvestigatorLogger investigatorLogger = new InvestigatorLogger(executorLog.getLogger());
        ctx.addExecutedListener(investigatorLogger::submit);
        return sqlClient.executor(
                ExecutorForLog.wrap(sqlClient.getExecutor(), investigatorLogger)
        );
    }
}
