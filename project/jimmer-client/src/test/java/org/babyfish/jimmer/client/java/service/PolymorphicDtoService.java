package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.java.model.dto.ClientView;
import org.babyfish.jimmer.client.meta.Api;

import java.util.List;

@Api("polymorphicDtoService")
public interface PolymorphicDtoService {

    @Api
    @GetMapping("/clients")
    List<ClientView> getClients();

    @Api
    @GetMapping("/manual-views")
    List<ManualView> getManualViews();

    interface ManualView {

        String name();

        class Branch implements ManualView {

            @Override
            public String name() {
                return null;
            }
        }
    }
}
