package org.babyfish.jimmer.dto.compiler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class URLDtoSource implements DtoSource {

    private final URL url;

    public URLDtoSource(URL url) {
        this.url = url;
    }

    @Override
    public String getName() {
        return url.toExternalForm();
    }

    @Override
    public Reader openReader() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
    }
}
