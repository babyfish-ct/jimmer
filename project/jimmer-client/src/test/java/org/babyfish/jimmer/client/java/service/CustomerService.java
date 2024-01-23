package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.common.PostMapping;
import org.babyfish.jimmer.client.common.RequestParam;
import org.babyfish.jimmer.client.common.RequestPart;
import org.babyfish.jimmer.client.java.model.Customer;
import org.babyfish.jimmer.client.java.model.Fetchers;
import org.babyfish.jimmer.client.java.model.dto.CustomerInput;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Api("customerService")
public interface CustomerService {

    @Api
    @GetMapping("/customers")
    Map<String, @FetchBy("DEFAULT_CUSTOMER") Customer> findCustomers(
            @Nullable @RequestParam(required = false) String name
    );

    @Api
    @PostMapping("/customer")
    Map<String, Integer> saveCustomer(
            @RequestPart(required = false) CustomerInput input,
            @RequestPart MultipartFile[] files
    );

    Fetcher<Customer> DEFAULT_CUSTOMER =
            Fetchers.CUSTOMER_FETCHER.allScalarFields();
}
