package org.babyfish.jimmer.dto.compiler;

import java.io.IOException;
import java.io.Reader;

public interface DtoSource {

    String getName();

    Reader openReader() throws IOException;
}
