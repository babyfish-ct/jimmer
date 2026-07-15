package org.babyfish.jimmer.spring.cfg;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = JimmerAutoConfiguration.class)
@Import(JimmerJacksonConfig.class)
public class JimmerJacksonAutoConfiguration {
}
