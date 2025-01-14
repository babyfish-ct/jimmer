package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.common.PostMapping;
import org.babyfish.jimmer.client.meta.Api;

@Api("recordService")
public class RecordService {
    @Api
    @PostMapping("/page/query")
    public PageQuery<String> pageQuery(PageQuery<String> pageQuery) {
        return pageQuery;
    }
}
