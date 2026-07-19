package org.babyfish.jimmer.dto.compiler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class DtoBundleLoader {

    public static final String ENABLED_OPTION = "jimmer.dto.bundle.enabled";

    public static final String MARKER_PATH = "META-INF/jimmer/dto-bundle.properties";

    private static final String PATH_PROPERTY = "path";

    private DtoBundleLoader() {
    }

    public static boolean isEnabled(Map<String, String> options) {
        String value = options.get(ENABLED_OPTION);
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("true")) {
            return true;
        }
        if (value.trim().equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException(
                "The processor option `" + ENABLED_OPTION + "` can only be \"true\" or \"false\""
        );
    }

    public static List<DtoFile> load(ClassLoader classLoader) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(MARKER_PATH);
        List<DtoFile> dtoFiles = new ArrayList<>();
        Set<String> loadedMarkers = new HashSet<>();
        while (resources.hasMoreElements()) {
            URL markerUrl = resources.nextElement();
            if (loadedMarkers.add(markerUrl.toExternalForm())) {
                load(markerUrl, dtoFiles);
            }
        }
        return Collections.unmodifiableList(dtoFiles);
    }

    private static void load(URL markerUrl, List<DtoFile> dtoFiles) throws IOException {
        String path = markerPath(markerUrl);
        switch (markerUrl.getProtocol()) {
            case "jar":
                loadJar(markerUrl, path, dtoFiles);
                break;
            case "file":
                loadDirectory(markerUrl, path, dtoFiles);
                break;
            default:
                throw new IOException(
                        "Cannot load DTO bundle marker \"" + markerUrl +
                                "\": unsupported URL protocol \"" + markerUrl.getProtocol() + '"'
                );
        }
    }

    private static String markerPath(URL markerUrl) throws IOException {
        Properties properties = new Properties();
        URLConnection connection = markerUrl.openConnection();
        connection.setUseCaches(false);
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String name : properties.stringPropertyNames()) {
            if (!PATH_PROPERTY.equals(name)) {
                throw new IOException(
                        "Illegal DTO bundle marker \"" + markerUrl +
                                "\": unsupported property \"" + name + '"'
                );
            }
        }
        String path = properties.getProperty(PATH_PROPERTY, "").trim();
        if (path.isEmpty()) {
            return "";
        }
        URI normalizedUri;
        try {
            normalizedUri = new URI(null, null, path, null).normalize();
        } catch (URISyntaxException ex) {
            throw illegalPath(markerUrl, path);
        }
        String normalizedPath = normalizedUri.getPath();
        if (normalizedPath.isEmpty()) {
            return "";
        }
        if (normalizedUri.isAbsolute() ||
                normalizedUri.getRawAuthority() != null ||
                normalizedPath.startsWith("/") ||
                normalizedPath.equals("..") ||
                normalizedPath.startsWith("../") ||
                normalizedPath.indexOf('\\') != -1) {
            throw illegalPath(markerUrl, path);
        }
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }

    private static IOException illegalPath(URL markerUrl, String path) {
        return new IOException(
                "Illegal DTO bundle marker \"" + markerUrl +
                        "\": path \"" + path + "\" must be a relative resource path"
        );
    }

    private static void loadJar(URL markerUrl, String path, List<DtoFile> dtoFiles) throws IOException {
        URLConnection connection = markerUrl.openConnection();
        connection.setUseCaches(false);
        if (!(connection instanceof JarURLConnection)) {
            throw new IOException("Cannot open DTO bundle marker \"" + markerUrl + "\" as a JAR resource");
        }
        JarURLConnection jarConnection = (JarURLConnection) connection;
        URL jarFileUrl = jarConnection.getJarFileURL();
        String prefix = path.isEmpty() ? "" : path + '/';
        boolean rootFound = path.isEmpty();
        List<String> entryNames = new ArrayList<>();
        try (JarFile jarFile = jarConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!prefix.isEmpty() && (entryName.equals(path) || entryName.startsWith(prefix))) {
                    rootFound = true;
                }
                if (!entry.isDirectory() && entryName.startsWith(prefix) && entryName.endsWith(".dto")) {
                    entryNames.add(entryName);
                }
            }
        }
        if (!rootFound) {
            throw new IOException(
                    "The DTO bundle marker \"" + markerUrl +
                            "\" points to missing resource directory \"" + path + '"'
            );
        }
        Collections.sort(entryNames);
        for (String entryName : entryNames) {
            String relativePath = entryName.substring(prefix.length());
            dtoFiles.add(dtoFile(jarEntryUrl(jarFileUrl, entryName), relativePath));
        }
    }

    private static URL jarEntryUrl(URL jarFileUrl, String entryName) throws IOException {
        try {
            String rawEntryName = new URI(null, null, '/' + entryName, null)
                    .getRawPath()
                    .substring(1);
            return URI.create(
                    "jar:" + jarFileUrl.toURI().toASCIIString() + "!/" + rawEntryName
            ).toURL();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new IOException("Illegal DTO bundle entry \"" + entryName + '"', ex);
        }
    }

    private static void loadDirectory(URL markerUrl, String path, List<DtoFile> dtoFiles) throws IOException {
        Path classpathRoot = filePath(markerUrl);
        for (String ignored : MARKER_PATH.split("/")) {
            classpathRoot = classpathRoot.getParent();
            if (classpathRoot == null) {
                throw new IOException("Illegal DTO bundle marker URL \"" + markerUrl + '"');
            }
        }
        Path dtoRoot = path.isEmpty() ? classpathRoot : classpathRoot.resolve(path);
        if (!Files.isDirectory(dtoRoot)) {
            throw new IOException(
                    "The DTO bundle marker \"" + markerUrl +
                            "\" points to missing resource directory \"" + path + '"'
            );
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dtoRoot)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(it -> it.getFileName().toString().endsWith(".dto"))
                    .forEach(files::add);
        }
        URI dtoRootUri = dtoRoot.toUri();
        files.sort(Comparator.comparing(it -> dtoRootUri.relativize(it.toUri()).getPath()));
        for (Path file : files) {
            String relativePath = dtoRootUri.relativize(file.toUri()).getPath();
            dtoFiles.add(dtoFile(file.toUri().toURL(), relativePath));
        }
    }

    private static Path filePath(URL url) throws IOException {
        try {
            URI uri = url.toURI();
            return Paths.get(uri);
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new IOException("Illegal DTO bundle marker URL \"" + url + '"', ex);
        }
    }

    private static DtoFile dtoFile(URL url, String relativePath) throws IOException {
        String[] parts = relativePath.split("/", -1);
        if (parts.length == 0 || parts[parts.length - 1].isEmpty()) {
            throw new IOException("Illegal DTO bundle entry \"" + relativePath + '"');
        }
        List<String> packagePaths = new ArrayList<>(parts.length - 1);
        for (int i = 0; i + 1 < parts.length; i++) {
            if (parts[i].isEmpty() || parts[i].equals(".") || parts[i].equals("..")) {
                throw new IOException("Illegal DTO bundle entry \"" + relativePath + '"');
            }
            packagePaths.add(parts[i]);
        }
        return new DtoFile(
                new URLDtoSource(url),
                "dto-bundle",
                "",
                packagePaths,
                parts[parts.length - 1]
        );
    }
}
