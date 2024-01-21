package io.quarkiverse.jimmer.it.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkiverse.jimmer.it.entity.Book;
import io.quarkiverse.jimmer.it.service.IBook;
import io.quarkus.agroal.runtime.DataSources;

@Path("/bookResource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResources {

    private final IBook iBook;

    public BookResources(IBook iBook) {
        this.iBook = iBook;
    }

    @GET
    @Path("/book")
    public Response getBookById(@RestQuery long id) {
        return Response.ok(iBook.findById(id)).build();
    }

    @POST
    @Path("/book")
    @Transactional(rollbackOn = Exception.class)
    public Response postBook(Book book) {
        return Response.ok(iBook.save(book)).build();
    }
}
