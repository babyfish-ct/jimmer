package org.babyfish.jimmer.client.java.model;

import com.fasterxml.jackson.annotation.JsonValue;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface BookStore {

    @Id
    @JsonConverter(LongToStringConverter.class)
    long id();

    String name();

    Level level();

    @OneToMany(mappedBy = "store")
    List<Book> books();

    enum Level {
        LOW,
        MIDDLE,
        HIGH;

        @JsonValue
        public int toInt() {
            switch (this) {
                case LOW:
                    return 10;
                case MIDDLE:
                    return 20;
                case HIGH:
                    return 30;
            }
            throw new AssertionError("Internal BUG");
        }
    }
}
