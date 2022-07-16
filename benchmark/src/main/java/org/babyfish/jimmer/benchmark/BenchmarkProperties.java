package org.babyfish.jimmer.benchmark;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "benchmark")
@ConstructorBinding
public class BenchmarkProperties {

    private final int dataCount;

    private final int repeatCount;

    public BenchmarkProperties(int dataCount, int repeatCount) {
        this.dataCount = dataCount;
        this.repeatCount = repeatCount;
    }

    public int getDataCount() {
        return dataCount;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    @Override
    public String toString() {
        return "BenchMarkProperties{" +
                "dataCount=" + dataCount +
                ", repeatCount=" + repeatCount +
                '}';
    }
}
