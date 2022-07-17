package org.babyfish.jimmer.benchmark;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class BenchmarkExecutor {

    @Autowired
    private BenchmarkProperties props;

    public abstract String name();

    public long execute() {

        query(); // Load & compile all necessary classes(like '@Warmup' of JMH, I don't know JMH before, so I did it manually)

        long millis = System.currentTimeMillis();
        for (int i = props.getRepeatCount(); i > 0; --i) {
            List<?> dataList = query();
            if (dataList.size() != props.getDataCount()) {
                throw new AssertionError(
                        "Expect data count is " +
                                props.getDataCount() +
                                ", actual data count is " +
                                dataList.size()
                );
            }
        }
        return System.currentTimeMillis() - millis;
    }

    protected abstract List<?> query();
}
