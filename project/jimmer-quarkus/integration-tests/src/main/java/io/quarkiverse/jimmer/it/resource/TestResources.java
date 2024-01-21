package io.quarkiverse.jimmer.it.resource;

import io.quarkiverse.jimmer.it.entity.Book;
import io.quarkiverse.jimmer.it.entity.UserRole;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.babyfish.jimmer.quarkus.runtime.Jimmer;
import org.babyfish.jimmer.sql.JSqlClient;

import java.util.List;


@Path("/testResources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResources {

    @Inject
    Jimmer jimmer;

    @Inject
    JSqlClient jSqlClient;

//    Another db in your configuration file
//    @Inject
//    @DataSource("DB2")
//    JSqlClient jSqlClient2;

    @GET
    @Path("/test")
    public Response test() {
        System.out.println("jimmerQuarkus = " + jimmer.getDefaultJSqlClient());
        System.out.println("jimmerQuarkus.getJSqlClient(\"DB2\") = " + jimmer.getJSqlClient("DB2"));

        List<InstanceHandle<JSqlClient>> instanceHandles = Arc.container().listAll(JSqlClient.class);
        for (InstanceHandle<JSqlClient> instanceHandle : instanceHandles) {
            System.out.println("instanceHandle = " + instanceHandle.get());
        }
//        System.out.println("jSqlClient2 = " + jSqlClient2);
//        UserRole userRole = jSqlClient2.findById(UserRole.class, "defc2d01-fb38-4d31-b006-fd182b25aa33");
//        System.out.println("userRole = " + userRole);
        Book book = jSqlClient.findById(Book.class, 1);
        return Response.ok(book).build();
    }
}
