package org.babyfish.jimmer.client.generator.openapi;

import java.io.StringWriter;
import java.util.*;

public class OpenApiProperties {

    private Info info;

    private List<Server> servers = Collections.emptyList();

    private List<Map<String, List<String>>> securities = Collections.emptyList();

    private Components components;

    public Info getInfo() {
        return info;
    }

    public OpenApiProperties setInfo(Info info) {
        this.info = info;
        return this;
    }

    public List<Server> getServers() {
        return servers;
    }

    public OpenApiProperties setServers(List<Server> servers) {
        this.servers = servers != null && !servers.isEmpty() ?
                Collections.unmodifiableList(servers) :
                Collections.emptyList();
        return this;
    }

    public List<Map<String, List<String>>> getSecurities() {
        return securities;
    }

    public OpenApiProperties setSecurities(List<Map<String, List<String>>> securities) {
        this.securities = securities != null && !securities.isEmpty() ?
                Collections.unmodifiableList(securities) :
                Collections.emptyList();
        return this;
    }

    public Components getComponents() {
        return components;
    }

    public OpenApiProperties setComponents(Components components) {
        this.components = components;
        return this;
    }

    public static abstract class Node {

        public abstract void writeTo(YmlWriter writer);

        @Override
        public String toString() {
            StringWriter writer = new StringWriter();
            YmlWriter ymlWriter = new YmlWriter(writer);
            writeTo(ymlWriter);
            return writer.toString();
        }

        public static void writeNodeTo(String objectName, Node node, YmlWriter writer) {
            if (node != null) {
                writer.object(objectName, () -> {
                    node.writeTo(writer);
                });
            }
        }
    }

    public static class Info extends Node {

        private String title;

        private String description;

        private String termsOfService;

        private Contact contact;

        private License license;

        private String version;

        public String getTitle() {
            return title;
        }

        public Info setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Info setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getTermsOfService() {
            return termsOfService;
        }

        public Info setTermsOfService(String termsOfService) {
            this.termsOfService = termsOfService;
            return this;
        }

        public Contact getContact() {
            return contact;
        }

        public Info setContact(Contact contact) {
            this.contact = contact;
            return this;
        }

        public License getLicense() {
            return license;
        }

        public Info setLicense(License license) {
            this.license = license;
            return this;
        }

        public String getVersion() {
            return version;
        }

        public Info setVersion(String version) {
            this.version = version;
            return this;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writer.prop("title", title);
            writer.description(Description.of(description));
            writer.prop("termsOfService", termsOfService);
            writeNodeTo("contact", contact, writer);
            writeNodeTo("license", license, writer);
            writer.prop("version", version);
        }
    }

    public static class Contact extends Node {

        private String name;

        private String url;

        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writer.prop("name", name);
            writer.prop("url", url);
            writer.prop("email", email);
        }
    }

    public  static class License extends Node {

        private String name;

        private String identifier;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writer.prop("name", name);
            writer.prop("identifier", identifier);
        }
    }

    public static class Server extends Node {

        private String url;

        private String description;

        private Map<String, Variable> variables = Collections.emptyMap();

        public String getUrl() {
            return url;
        }

        public Server setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Server setDescription(String description) {
            this.description = description;
            return this;
        }

        public Map<String, Variable> getVariables() {
            return variables;
        }

        public Server setVariables(Map<String, Variable> variables) {
            this.variables = variables != null && !variables.isEmpty() ?
                    Collections.unmodifiableMap(variables) :
                    Collections.emptyMap();
            return this;
        }

        @Override
        public void writeTo(YmlWriter writer) {
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
    }

    public static class Variable extends Node {

        private List<String> enums = Collections.emptyList();

        private String defaultValue;

        private String description;

        public List<String> getEnums() {
            return enums;
        }

        public void setEnums(List<String> enums) {
            this.enums = enums != null && enums.isEmpty() ? enums : Collections.emptyList();
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public void writeTo(YmlWriter writer) {
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

    public static class Components extends Node {

        private Map<String, SecurityScheme> securitySchemes = Collections.emptyMap();

        public Map<String, SecurityScheme> getSecuritySchemes() {
            return securitySchemes;
        }

        public Components setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes != null && !securitySchemes.isEmpty() ?
                Collections.unmodifiableMap(securitySchemes) :
                Collections.emptyMap();
            return this;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writer.object("securitySchemes", () -> {
                for (Map.Entry<String, SecurityScheme> e : securitySchemes.entrySet()) {
                    writer.object(e.getKey(), () -> {
                        e.getValue().writeTo(writer);
                    });
                }
            });
        }
    }

    public static class SecurityScheme extends Node {

        private String type;

        private String description;

        private String name;

        private In in = In.HEADER;

        private String scheme;

        private String bearerFormat;

        private Flows flows;

        private String openIdConnectUrl;

        public String getType() {
            return type;
        }

        public SecurityScheme setType(String type) {
            this.type = type;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public SecurityScheme setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getName() {
            return name;
        }

        public SecurityScheme setName(String name) {
            this.name = name;
            return this;
        }

        public In getIn() {
            return in;
        }

        public SecurityScheme setIn(In in) {
            this.in = in != null ? in : In.HEADER;
            return this;
        }

        public String getScheme() {
            return scheme;
        }

        public SecurityScheme setScheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public String getBearerFormat() {
            return bearerFormat;
        }

        public SecurityScheme setBearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        public Flows getFlows() {
            return flows;
        }

        public SecurityScheme setFlows(Flows flows) {
            this.flows = flows;
            return this;
        }

        public String getOpenIdConnectUrl() {
            return openIdConnectUrl;
        }

        public SecurityScheme setOpenIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writer.prop("type", type);
            writer.prop("description", description);
            writer.prop("name", name);
            writer.prop("in", in.name().toLowerCase());
            writer.prop("scheme", scheme);
            writer.prop("bearerFormat", bearerFormat);
            writeNodeTo("flows", flows, writer);
            writer.prop("openIdConnectUrl", openIdConnectUrl);
        }
    }

    public static class Flows extends Node {

        private Flow implicit;

        private Flow password;

        private Flow clientCredentials;

        private Flow authorizationCode;

        public Flow getImplicit() {
            return implicit;
        }

        public void setImplicit(Flow implicit) {
            this.implicit = implicit;
        }

        public Flow getPassword() {
            return password;
        }

        public void setPassword(Flow password) {
            this.password = password;
        }

        public Flow getClientCredentials() {
            return clientCredentials;
        }

        public void setClientCredentials(Flow clientCredentials) {
            this.clientCredentials = clientCredentials;
        }

        public Flow getAuthorizationCode() {
            return authorizationCode;
        }

        public void setAuthorizationCode(Flow authorizationCode) {
            this.authorizationCode = authorizationCode;
        }

        @Override
        public void writeTo(YmlWriter writer) {
            writeNodeTo("implicit", implicit, writer);
            writeNodeTo("password", password, writer);
            writeNodeTo("clientCredentials", clientCredentials, writer);
            writeNodeTo("authorizationCode", authorizationCode, writer);
        }
    }

    public static class Flow extends Node {

        private String authorizationUrl;

        private String tokenUrl;

        private String refreshUrl;

        private Map<String, String> scopes = Collections.emptyMap();

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getRefreshUrl() {
            return refreshUrl;
        }

        public void setRefreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
        }

        public Map<String, String> getScopes() {
            return scopes;
        }

        public void setScopes(Map<String, String> scopes) {
            this.scopes = scopes != null && !scopes.isEmpty() ?
                    Collections.unmodifiableMap(scopes) :
                    Collections.emptyMap();
        }

        @Override
        public void writeTo(YmlWriter writer) {
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

    public enum In {
        QUERY, HEADER, COOKIE
    }
}
