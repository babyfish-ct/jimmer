package org.babyfish.jimmer.client.generator.openapi;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpenApiProperties {

    private final Info info;

    private final List<Server> servers;

    private final Map<String, List<Object>> securityMap;

    private final Components components;

    public OpenApiProperties(Info info, List<Server> servers, Map<String, List<Object>> securityMap, Components components) {
        this.info = info;
        this.servers = servers != null && !servers.isEmpty() ? Collections.unmodifiableList(servers) : Collections.emptyList();
        this.securityMap = securityMap != null && !securityMap.isEmpty() ? Collections.unmodifiableMap(securityMap) : Collections.emptyMap();
        this.components = components;
    }

    public Info getInfo() {
        return info;
    }

    public List<Server> getServers() {
        return servers;
    }

    public Map<String, List<Object>> getSecurityMap() {
        return securityMap;
    }

    public Components getComponents() {
        return components;
    }

    public static class Info extends Node {

        private final String title;

        private final String description;

        private final String termsOfService;

        private final Concat contact;

        private final License license;

        private final String version;

        public Info(String title, String description, String termsOfService, Concat contact, License license, String version) {
            this.title = title;
            this.description = description;
            this.termsOfService = termsOfService;
            this.contact = contact;
            this.license = license;
            this.version = version;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getTermsOfService() {
            return termsOfService;
        }

        public Concat getContact() {
            return contact;
        }

        public License getLicense() {
            return license;
        }

        public String getVersion() {
            return version;
        }

        @Override
        protected void writeTo(YmlWriter writer) {
            writer.prop("title", title);
            writer.description(Description.of(description));
            writer.prop("termsOfService", termsOfService);
            writeNodeTo("contact", contact, writer);
            writeNodeTo("license", license, writer);
            writer.prop("version", version);
        }

        public static class Concat extends Node {

            private final String name;

            private final String url;

            private final String email;

            public Concat(String name, String url, String email) {
                this.name = name;
                this.url = url;
                this.email = email;
            }

            public String getName() {
                return name;
            }

            public String getUrl() {
                return url;
            }

            public String getEmail() {
                return email;
            }

            @Override
            protected void writeTo(YmlWriter writer) {
                writer.prop("name", name);
                writer.prop("url", url);
                writer.prop("email", email);
            }
        }

        public  static class License extends Node {

            private final String name;

            private final String identifier;

            private License(String name, String identifier) {
                this.name = name;
                this.identifier = identifier;
            }

            public String getName() {
                return name;
            }

            public String getIdentifier() {
                return identifier;
            }

            @Override
            protected void writeTo(YmlWriter writer) {
                writer.prop("name", name);
                writer.prop("identifier", identifier);
            }
        }
    }

    public static class Server extends Node {

        private final String url;

        private final String description;

        private final Map<String, Variable> variables;

        public Server(String url, String description, Map<String, Variable> variables) {
            this.url = url;
            this.description = description;
            this.variables = variables != null && !variables.isEmpty() ? Collections.unmodifiableMap(variables) : Collections.emptyMap();
        }

        public String getUrl() {
            return url;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Variable> getVariables() {
            return variables;
        }

        @Override
        protected void writeTo(YmlWriter writer) {
            writer.prop("url", url);
            writer.prop("description", description);
            if (!variables.isEmpty()) {
                for (Map.Entry<String, Variable> e : variables.entrySet()) {
                    writer.object(e.getKey(), () -> {
                       e.getValue().writeTo(writer);
                    });
                }
            }
        }

        public static class Variable extends Node {

            private final List<String> enums;

            private final String defaultValue;

            private final String description;

            public Variable(List<String> enums, String defaultValue, String description) {
                this.enums = enums != null && !enums.isEmpty() ? Collections.unmodifiableList(enums) : Collections.emptyList();
                this.defaultValue = defaultValue;
                this.description = description;
            }

            public List<String> getEnums() {
                return enums;
            }

            public String getDefaultValue() {
                return defaultValue;
            }

            public String getDescription() {
                return description;
            }

            @Override
            protected void writeTo(YmlWriter writer) {
                if (!enums.isEmpty()) {
                    writer.list("enum", () -> {
                       for (String en : enums) {
                           writer.listItem(() -> writer.code(en).code('\n'));
                       }
                    });
                }
                writer.prop("default", defaultValue);
                writer.prop("description", description);
            }
        }
    }

    public static class Components extends Node {

        private final List<SecurityScheme> securitySchemes;

        public Components(List<SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes != null && !securitySchemes.isEmpty() ?
                    Collections.unmodifiableList(securitySchemes) :
                    Collections.emptyList();
        }

        public List<SecurityScheme> getSecuritySchemes() {
            return securitySchemes;
        }

        @Override
        protected void writeTo(YmlWriter writer) {
            writer.list("securitySchemes", () -> {
                for (SecurityScheme securityScheme : securitySchemes) {
                    writer.listItem(() -> {
                        securityScheme.writeTo(writer);
                    });
                }
            });
        }

        public static class SecurityScheme extends Node {

            private final String type;

            private final String description;

            private final String name;

            private final In in;

            private final String scheme;

            private final String bearerFormat;

            private final Flows flows;

            private final String openIdConnectUrl;

            private SecurityScheme(String type, String description, String name, In in, String scheme, String bearerFormat, Flows flows, String openIdConnectUrl) {
                this.type = type;
                this.description = description;
                this.name = name;
                this.in = in;
                this.scheme = scheme;
                this.bearerFormat = bearerFormat;
                this.flows = flows;
                this.openIdConnectUrl = openIdConnectUrl;
            }

            public String getType() {
                return type;
            }

            public String getDescription() {
                return description;
            }

            public String getName() {
                return name;
            }

            public In getIn() {
                return in;
            }

            public String getScheme() {
                return scheme;
            }

            public String getBearerFormat() {
                return bearerFormat;
            }

            public Flows getFlows() {
                return flows;
            }

            public String getOpenIdConnectUrl() {
                return openIdConnectUrl;
            }

            @Override
            protected void writeTo(YmlWriter writer) {
                writer.prop("type", type);
                writer.prop("description", description);
                writer.prop("name", name);
                writer.prop("in", in.name().toLowerCase());
                writer.prop("scheme", scheme);
                writer.prop("bearerFormat", bearerFormat);
                writeNodeTo("flows", flows, writer);
                writer.prop("openIdConnectUrl", openIdConnectUrl);
            }

            private static class Flows extends Node {

                private final Flow implicit;

                private final Flow password;

                private final Flow clientCredentials;

                private final Flow authorizationCode;

                private Flows(Flow implicit, Flow password, Flow clientCredentials, Flow authorizationCode) {
                    this.implicit = implicit;
                    this.password = password;
                    this.clientCredentials = clientCredentials;
                    this.authorizationCode = authorizationCode;
                }

                public Flow getImplicit() {
                    return implicit;
                }

                public Flow getPassword() {
                    return password;
                }

                public Flow getClientCredentials() {
                    return clientCredentials;
                }

                public Flow getAuthorizationCode() {
                    return authorizationCode;
                }

                @Override
                protected void writeTo(YmlWriter writer) {
                    writeNodeTo("implicit", implicit, writer);
                    writeNodeTo("password", password, writer);
                    writeNodeTo("clientCredentials", clientCredentials, writer);
                    writeNodeTo("authorizationCode", authorizationCode, writer);
                }

                private static class Flow extends Node {

                    private final String authorizationUrl;

                    private final String tokenUrl;

                    private final String refreshUrl;

                    private Map<String, String> scopes;

                    private Flow(String authorizationUrl, String tokenUrl, String refreshUrl, Map<String, String> scopes) {
                        this.authorizationUrl = authorizationUrl;
                        this.tokenUrl = tokenUrl;
                        this.refreshUrl = refreshUrl;
                        this.scopes = scopes != null && !scopes.isEmpty() ?
                                Collections.unmodifiableMap(scopes) :
                                Collections.emptyMap();
                    }

                    public String getAuthorizationUrl() {
                        return authorizationUrl;
                    }

                    public String getTokenUrl() {
                        return tokenUrl;
                    }

                    public String getRefreshUrl() {
                        return refreshUrl;
                    }

                    public Map<String, String> getScopes() {
                        return scopes;
                    }

                    @Override
                    protected void writeTo(YmlWriter writer) {
                        writer.prop("authorizationUrl", authorizationUrl);
                        writer.prop("tokenUrl", tokenUrl);
                        writer.prop("refreshUrl", refreshUrl);
                        writer.object("scopes", () -> {
                            for (Map.Entry<String, String> e : scopes.entrySet()) {
                                writer.prop(e.getKey(), e.getValue());
                            }
                        });
                    }
                }
            }
        }
    }

    public static abstract class Node {

        protected abstract void writeTo(YmlWriter writer);

        @Override
        public String toString() {
            StringWriter writer = new StringWriter();
            YmlWriter ymlWriter = new YmlWriter(writer);
            writeTo(ymlWriter);
            return writer.toString();
        }

        protected static void writeNodeTo(String objectName, Node node, YmlWriter writer) {
            if (node != null) {
                writer.object(objectName, () -> {
                    node.writeTo(writer);
                });
            }
        }
    }

    public enum In {
        QUERY, HEADER, COOKIE
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static InfoBuilder newInfoBuilder() {
        return new InfoBuilder();
    }

    public static class Builder {

        private Info info;

        private List<Server> servers;

        private Map<String, List<Object>> securityMap;

        private Components components;

        public Builder setInfo(Info info) {
            this.info = info;
            return this;
        }

        public Builder setServers(List<Server> servers) {
            this.servers = servers;
            return this;
        }

        public Builder setSecurityMap(Map<String, List<Object>> securityMap) {
            this.securityMap = securityMap;
            return this;
        }

        public Builder setComponents(Components components) {
            this.components = components;
            return this;
        }

        public OpenApiProperties build() {
            return new OpenApiProperties(info, servers, securityMap, components);
        }
    }

    public static class InfoBuilder {

        private String title;

        private String description;

        private String termsOfService;

        private Info.Concat contact;

        private Info.License license;

        private String version;

        public InfoBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public InfoBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public InfoBuilder setTermsOfService(String termsOfService) {
            this.termsOfService = termsOfService;
            return this;
        }

        public InfoBuilder setContact(Info.Concat contact) {
            this.contact = contact;
            return this;
        }

        public InfoBuilder setLicense(Info.License license) {
            this.license = license;
            return this;
        }

        public InfoBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Info build() {
            return new Info(title, description, termsOfService, contact, license, version);
        }
    }
}
