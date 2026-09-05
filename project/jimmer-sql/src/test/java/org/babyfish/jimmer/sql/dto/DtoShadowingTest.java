package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.dto.ShadowingArrayView;
import org.babyfish.jimmer.sql.model.dto.ShadowingInput;
import org.babyfish.jimmer.sql.model.dto.ShadowingPrimitiveView;
import org.babyfish.jimmer.sql.model.dto.ShadowingValidationInput;
import org.babyfish.jimmer.sql.model.dto.ShadowingView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class DtoShadowingTest {

    @Test
    public void testHibernateValidatorAccess() {
        ShadowingValidationInput view = new ShadowingValidationInput();
        view.setName("book");
        assertEquals("book", view.$$_hibernateValidator_getFieldValue("name"));
        assertEquals("book", view.$$_hibernateValidator_getGetterValue("getName"));
        view.setName(null);
        assertNull(view.$$_hibernateValidator_getFieldValue("name"));
        assertNull(view.$$_hibernateValidator_getGetterValue("getName"));
        assertThrows(IllegalArgumentException.class, () -> view.$$_hibernateValidator_getFieldValue("missing"));
        assertThrows(IllegalArgumentException.class, () -> view.$$_hibernateValidator_getGetterValue("missing"));
    }

    @Test
    public void testEqualsHashCodeAndToString() {
        ShadowingView view = view();
        assertEquals(view, view());
        ShadowingView different = view();
        different.setO("different");
        assertNotEquals(view, different);
        different = view();
        different.setOther("different");
        assertNotEquals(view, different);
        int hash = "book".hashCode();
        for (String value : Arrays.asList("hash value", "o value", "other value", "builder value")) {
            hash = hash * 31 + value.hashCode();
        }
        assertEquals(hash, view.hashCode());
        assertEquals(
                "ShadowingView(name=book, hash=hash value, o=o value, other=other value, builder=builder value)",
                view.toString()
        );

        ShadowingArrayView arrayView = new ShadowingArrayView();
        arrayView.setHash(new String[] {"one", "two"});
        assertEquals(Arrays.hashCode(new String[] {"one", "two"}), arrayView.hashCode());
        ShadowingArrayView sameArrayView = new ShadowingArrayView();
        sameArrayView.setHash(new String[] {"one", "two"});
        assertEquals(arrayView, sameArrayView);

        ShadowingPrimitiveView primitiveView = new ShadowingPrimitiveView();
        primitiveView.setHash(7);
        assertEquals(Integer.hashCode(7), primitiveView.hashCode());
    }

    @Test
    public void testInputBuilderAndConditionalToString() {
        ShadowingInput empty = new ShadowingInput.Builder().build();
        assertEquals("ShadowingInput(_sp=null, _input=null)", empty.toString());

        ShadowingInput input = new ShadowingInput.Builder()
                .builder("book")
                ._sp("separator value")
                ._input("input value")
                .build();
        assertEquals("book", input.getBuilder());
        assertEquals("separator value", input.get_sp());
        assertEquals("input value", input.get_input());
        assertEquals("ShadowingInput(builder=book, _sp=separator value, _input=input value)", input.toString());
        assertEquals("book", input.toEntity().name());
    }

    private static ShadowingView view() {
        ShadowingView view = new ShadowingView();
        view.setName("book");
        view.setHash("hash value");
        view.setO("o value");
        view.setOther("other value");
        view.setBuilder("builder value");
        return view;
    }
}
