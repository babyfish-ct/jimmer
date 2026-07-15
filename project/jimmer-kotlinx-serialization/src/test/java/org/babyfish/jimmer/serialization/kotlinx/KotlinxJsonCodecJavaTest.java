package org.babyfish.jimmer.serialization.kotlinx;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.serialization.kotlinx.model.SerializableBook;
import org.babyfish.jimmer.serialization.kotlinx.model.dto.SerializableBookView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KotlinxJsonCodecJavaTest {

    @Test
    public void javaCodeCanUseKotlinxCodecForImmutableObjects() {
        KotlinxJsonCodec codec = new KotlinxJsonCodec();
        SerializableBook book = ImmutableObjects.fromString(
                SerializableBook.class,
                "{\"id\":3,\"name\":\"Java caller\"}",
                codec
        );

        assertEquals(
                "{\"id\":3,\"name\":\"Java caller\"}",
                ImmutableObjects.toString(book, codec)
        );
    }

    @Test
    public void javaCodeCanUseKotlinxCodecForGeneratedDto() throws Exception {
        KotlinxJsonCodec codec = new KotlinxJsonCodec();
        SerializableBookView view = new SerializableBookView(4L, "Java DTO");

        String json = codec.writerFor(SerializableBookView.class).writeAsString(view);
        SerializableBookView decoded = codec.readerFor(SerializableBookView.class).read(json);

        assertEquals("{\"id\":4,\"name\":\"Java DTO\"}", json);
        assertEquals(view, decoded);
    }
}
