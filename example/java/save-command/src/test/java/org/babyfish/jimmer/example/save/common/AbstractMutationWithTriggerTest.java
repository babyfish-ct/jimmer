package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.junit.jupiter.api.BeforeEach;

public class TriggerTest extends AbstractMutationTest {

    @Override
    protected void customize(JSqlClient.Builder builder) {
        builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
    }

    @BeforeEach
    public void registerEventListeners() {

    }
}
