package org.babyfish.jimmer.client.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface TypeDefinition {

    TypeName getTypeName();

    Kind getKind();

    boolean isApiIgnore();

    @Nullable
    Doc getDoc();

    @Nullable
    Error getError();

    boolean isRealModuleType();

    @Nullable
    List<String> getGroups();

    Map<String, Prop> getPropMap();

    List<TypeRef> getSuperTypes();

    Map<String, EnumConstant> getEnumConstantMap();

    enum Kind {
        IMMUTABLE,
        OBJECT,
        ENUM
    }

    final class Error {

        private final String family;

        private final String code;

        @JsonCreator
        public Error(@JsonProperty("family") String family, @JsonProperty("code") String code) {
            this.family = family;
            this.code = code;
        }

        public String getFamily() {
            return family;
        }

        public String getCode() {
            return code;
        }

        @Override
        public int hashCode() {
            int result = family.hashCode();
            result = 31 * result + code.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Error error = (Error) o;

            if (!family.equals(error.family)) return false;
            return code.equals(error.code);
        }

        @Override
        public String toString() {
            return "Error{" +
                    "family='" + family + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }
    }
}
