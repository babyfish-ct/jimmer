package org.babyfish.jimmer.client.generator.openapi;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.io.StringWriter;
import java.util.*;

@ConfigurationProperties("jimmer.client.openapi.properties")
@ConstructorBinding
public class OpenApiProperties {

    private final Info info;

    private final List<Server> servers;

    private final List<Map<String, List<String>>> securities;

    private final Components components;

    public OpenApiProperties(
            Info info, 
            List<Server> servers, 
            List<Map<String, List<String>>> securities, 
            Components components
    ) {
        this.info = info;
        this.servers = servers != null && !servers.isEmpty() ?
                Collections.unmodifiableList(servers) :
                Collections.emptyList();
        this.securities = securities != null && !securities.isEmpty() ?
                Collections.unmodifiableList(securities) :
                Collections.emptyList();
        this.components = components;
    }

    public Info getInfo() {
        return info;
    }

    public List<Server> getServers() {
        return servers;
    }

    public List<Map<String, List<String>>> getSecurities() {
        return securities;
    }

    public Components getComponents() {
        return components;
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

    @ConstructorBinding
    public static class Info extends Node {

        private final String title;

        private final String description;

        private final String termsOfService;

        private final Contact contact;

        private final License license;

        private final String version;

        public Info(String title, String description, String termsOfService, Contact contact, License license, String version) {
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

        public Contact getContact() {
            return contact;
        }

        public License getLicense() {
            return license;
        }

        public String getVersion() {
            return version;
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

    @ConstructorBinding
    public static class Contact extends Node {

        private final String name;

        private final String url;

        private final String email;

        public Contact(String name, String url, String email) {
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
        public void writeTo(YmlWriter writer) {
            writer.prop("name", name);
            writer.prop("url", url);
            writer.prop("email", email);
        }
    }

    @ConstructorBinding
    public  static class License extends Node {

        private final String name;

        private final String identifier;

        public License(String name, String identifier) {
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
        public void writeTo(YmlWriter writer) {
            writer.prop("name", name);
            writer.prop("identifier", identifier);
        }
    }

    @ConstructorBinding
    public static class Server extends Node {

        private final String url;

        private final String description;

        private final Map<String, Variable> variables;

        public Server(String url, String description, Map<String, Variable> variables) {
            this.url = url;
            this.description = description;
            this.variables = variables != null && !variables.isEmpty() ?
                    Collections.unmodifiableMap(variables) :
                    Collections.emptyMap();
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

    @ConstructorBinding
    public static class Variable extends Node {

        private final List<String> enums;

        private final String defaultValue;

        private final String description;

        public Variable(List<String> enums, String defaultValue, String description) {
            this.enums = enums != null && !enums.isEmpty() ?
                    Collections.unmodifiableList(enums) :
                    Collections.emptyList();
            this.defaultValue = defaultValue;
            this.description = description;
        }

        @NotNull
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

    @ConstructorBinding
    public static class Components extends Node {

        private final Map<String, SecurityScheme> securitySchemes;

        public Components(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes =
                    securitySchemes != null && !securitySchemes.isEmpty() ?
                            Collections.unmodifiableMap(securitySchemes) :
                            Collections.emptyMap();
        }

        public Map<String, SecurityScheme> getSecuritySchemes() {
            return securitySchemes;
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

    @ConstructorBinding
    public static class SecurityScheme extends Node {

        private final String type;

        private final String description;

        private final String name;

        private final In in;

        private final String scheme;

        private final String bearerFormat;

        private final Flows flows;

        private final String openIdConnectUrl;

        public SecurityScheme(
                String type,
                String description,
                String name,
                In in,
                String scheme,
                String bearerFormat,
                Flows flows,
                String openIdConnectUrl
        ) {
            this.type = type;
            this.description = description;
            this.name = name;
            this.in = in != null ? in : In.HEADER;
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

    @ConstructorBinding
    public static class Flows extends Node {

        private final Flow implicit;

        private final Flow password;

        private final Flow clientCredentials;

        private final Flow authorizationCode;

        public Flows(Flow implicit, Flow password, Flow clientCredentials, Flow authorizationCode) {
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
        public void writeTo(YmlWriter writer) {
            writeNodeTo("implicit", implicit, writer);
            writeNodeTo("password", password, writer);
            writeNodeTo("clientCredentials", clientCredentials, writer);
            writeNodeTo("authorizationCode", authorizationCode, writer);
        }
    }

    @ConstructorBinding
    public static class Flow extends Node {

        private final String authorizationUrl;

        private final String tokenUrl;

        private final String refreshUrl;

        private final Map<String, String> scopes;

        public Flow(String authorizationUrl, String tokenUrl, String refreshUrl, Map<String, String> scopes) {
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

    //-----------------

    public static Builder newBuilder() {
        return new Builder(null);
    }

    public static Builder newBuilder(OpenApiProperties properties) {
        return new Builder(properties);
    }

    public static InfoBuilder newInfoBuilder() {
        return new InfoBuilder(null);
    }

    public static InfoBuilder newInfoBuilder(Info info) {
        return new InfoBuilder(info);
    }

    public static ContactBuilder newContactBuilder() {
        return new ContactBuilder(null);
    }

    public static ContactBuilder newContactBuilder(Contact contact) {
        return new ContactBuilder(contact);
    }

    public static LicenseBuilder newLicenseBuilder() {
        return new LicenseBuilder(null);
    }

    public static LicenseBuilder newLicenseBuilder(License license) {
        return new LicenseBuilder(license);
    }

    public static ServerBuilder newServerBuilder() {
        return new ServerBuilder(null);
    }

    public static ServerBuilder newServiceBuilder(Server server) {
        return new ServerBuilder(server);
    }

    public static VariableBuilder newVariableBuilder() {
        return new VariableBuilder(null);
    }

    public static VariableBuilder newVariableBuilder(Variable variable) {
        return new VariableBuilder(variable);
    }

    public static ComponentsBuilder newComponentsBuilder() {
        return new ComponentsBuilder(null);
    }

    public static ComponentsBuilder newComponentsBuilder(Components components) {
        return new ComponentsBuilder(components);
    }

    public static SecuritySchemeBuilder newSecuritySchemeBuilder() {
        return new SecuritySchemeBuilder(null);
    }

    public static SecuritySchemeBuilder newSecuritySchemeBuilder(SecurityScheme securityScheme) {
        return new SecuritySchemeBuilder(securityScheme);
    }

    public static FlowsBuilder newFlowsBuilder() {
        return new FlowsBuilder(null);
    }

    public static FlowsBuilder newFlowsBuilder(Flows flows) {
        return new FlowsBuilder(flows);
    }

    public static FlowBuilder newFlowBuilder() {
        return new FlowBuilder(null);
    }

    public static FlowBuilder newFlowBuilder(Flow flow) {
        return new FlowBuilder(flow);
    }

    public static class Builder {

        private Info info;

        private List<Server> servers;

        private List<Map<String, List<String>>> securities;

        private Components components;

        Builder(OpenApiProperties properties) {
            if (properties != null) {
                this.info = properties.getInfo();
                this.servers = properties.getServers();
                this.securities = properties.getSecurities();
                this.components = properties.getComponents();
            }
        }

        public Builder setInfo(Info info) {
            this.info = info;
            return this;
        }

        public Builder setServers(List<Server> servers) {
            this.servers = servers;
            return this;
        }

        public Builder setSecurities(List<Map<String, List<String>>> securities) {
            this.securities = securities;
            return this;
        }

        public Builder setComponents(Components components) {
            this.components = components;
            return this;
        }

        public OpenApiProperties build() {
            return new OpenApiProperties(
                    info,
                    servers,
                    securities,
                    components
            );
        }
    }

    public static class InfoBuilder {

        private String title;

        private String description;

        private String termsOfService;

        private Contact contact;

        private License license;

        private String version;

        InfoBuilder(Info info) {
            if (info != null) {
                title = info.getTitle();
                description = info.getDescription();
                termsOfService = info.getTermsOfService();
                contact = info.getContact();
                license = info.getLicense();
                version = info.getVersion();
            }
        }

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

        public InfoBuilder setContact(Contact contact) {
            this.contact = contact;
            return this;
        }

        public InfoBuilder setLicense(License license) {
            this.license = license;
            return this;
        }

        public InfoBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Info build() {
            return new Info(
                    title,
                    description,
                    termsOfService,
                    contact,
                    license,
                    version
            );
        }
    }

    public static class ContactBuilder {

        private String name;

        private String url;

        private String email;

        ContactBuilder(Contact contact) {
            if (contact != null) {
                name = contact.getName();
                url = contact.getUrl();
                email = contact.getEmail();
            }
        }

        public ContactBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public ContactBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public ContactBuilder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Contact build() {
            return new Contact(name, url, email);
        }
    }

    public static class LicenseBuilder {

        private String name;

        private String identifier;

        LicenseBuilder(License license) {
            if (license != null) {
                name = license.getName();
                identifier = license.getIdentifier();
            }
        }

        public LicenseBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public LicenseBuilder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public License build() {
            return new License(name, identifier);
        }
    }

    public static class ServerBuilder {

        private String url;

        private String description;

        private Map<String, Variable> variables;

        ServerBuilder(Server server) {
            if (server != null) {
                url = server.getUrl();
                description = server.getDescription();
                variables = server.getVariables();
            }
        }

        public ServerBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public ServerBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public ServerBuilder setVariables(Map<String, Variable> variables) {
            this.variables = variables;
            return this;
        }

        public Server build() {
            return new Server(url, description, variables);
        }
    }

    public static class VariableBuilder {

        private List<String> enums;

        private String defaultValue;

        private String description;

        VariableBuilder(Variable variable) {
            if (variable != null) {
                enums = variable.getEnums();
                defaultValue = variable.getDefaultValue();
                description = variable.getDescription();
            }
        }

        public VariableBuilder setEnums(List<String> enums) {
            this.enums = enums;
            return this;
        }

        public VariableBuilder setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public VariableBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Variable build() {
            return new Variable(enums, defaultValue, defaultValue);
        }
    }

    public static class ComponentsBuilder {

        private Map<String, SecurityScheme> securitySchemes;

        ComponentsBuilder(Components components) {
            if (components != null) {
                securitySchemes = components.getSecuritySchemes();
            }
        }

        public ComponentsBuilder setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        public Components build() {
            return new Components(securitySchemes);
        }
    }

    public static class SecuritySchemeBuilder {

        private String type;

        private String description;

        private String name;

        private In in;

        private String scheme;

        private String bearerFormat;

        private Flows flows;

        private String openIdConnectUrl;

        SecuritySchemeBuilder(SecurityScheme securityScheme) {
            if (securityScheme != null) {
                type = securityScheme.getType();
                description = securityScheme.getDescription();
                name = securityScheme.getName();
                in = securityScheme.getIn();
                scheme = securityScheme.getScheme();
                bearerFormat = securityScheme.getBearerFormat();
                flows = securityScheme.getFlows();
                openIdConnectUrl = securityScheme.getOpenIdConnectUrl();
            }
        }

        public SecuritySchemeBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public SecuritySchemeBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public SecuritySchemeBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SecuritySchemeBuilder setIn(In in) {
            this.in = in;
            return this;
        }

        public SecuritySchemeBuilder setScheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public SecuritySchemeBuilder setBearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        public SecuritySchemeBuilder setFlows(Flows flows) {
            this.flows = flows;
            return this;
        }

        public SecuritySchemeBuilder setOpenIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        public SecurityScheme build() {
            return new SecurityScheme(
                    type,
                    description,
                    name,
                    in,
                    scheme,
                    bearerFormat,
                    flows,
                    openIdConnectUrl
            );
        }
    }

    public static class FlowsBuilder  {

        private Flow implicit;

        private Flow password;

        private Flow clientCredentials;

        private Flow authorizationCode;

        FlowsBuilder(Flows flows) {
            implicit = flows.getImplicit();
            password = flows.getPassword();
            clientCredentials = flows.getClientCredentials();
            authorizationCode = flows.getAuthorizationCode();
        }

        public FlowsBuilder setImplicit(Flow implicit) {
            this.implicit = implicit;
            return this;
        }

        public FlowsBuilder setPassword(Flow password) {
            this.password = password;
            return this;
        }

        public FlowsBuilder setClientCredentials(Flow clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        public FlowsBuilder setAuthorizationCode(Flow authorizationCode) {
            this.authorizationCode = authorizationCode;
            return this;
        }

        public Flows build() {
            return new Flows(
                    implicit,
                    password,
                    clientCredentials,
                    authorizationCode
            );
        }
    }

    public static class FlowBuilder {

        private String authorizationUrl;

        private String tokenUrl;

        private String refreshUrl;

        private Map<String, String> scopes;

        FlowBuilder(Flow flow) {
            if (flow != null) {
                authorizationUrl = flow.getAuthorizationUrl();
                tokenUrl = flow.getTokenUrl();
                refreshUrl = flow.getRefreshUrl();
                scopes = flow.getScopes();
            }
        }

        public FlowBuilder setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
            return this;
        }

        public FlowBuilder setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        public FlowBuilder setRefreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
            return this;
        }

        public FlowBuilder setScopes(Map<String, String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Flow build() {
            return new Flow(
                    authorizationUrl,
                    tokenUrl,
                    refreshUrl,
                    scopes
            );
        }
    }
}
