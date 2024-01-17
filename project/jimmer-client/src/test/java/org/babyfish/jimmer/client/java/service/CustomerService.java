package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.common.RequestParam;
import org.babyfish.jimmer.client.java.model.Customer;
import org.babyfish.jimmer.client.java.model.Fetchers;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Api("customerService")
public interface CustomerService {

    @Api
    Map<String, @FetchBy("DEFAULT_CUSTOMER") Customer> findCustomers(
            @Nullable @RequestParam String name
    );

    Fetcher<Customer> DEFAULT_CUSTOMER =
            Fetchers.CUSTOMER_FETCHER.allScalarFields();
}
