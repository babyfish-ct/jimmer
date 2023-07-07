package org.babyfish.jimmer.benchmark.easyquery;

import com.easy.query.core.api.client.EasyQueryClient;

import java.util.List;

/**
 * create time 2023/6/18 21:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyQueryTest {
    private final EasyQueryClient easyQueryClient;

    public EasyQueryTest(EasyQueryClient easyQueryClient){

        this.easyQueryClient = easyQueryClient;
    }
    public List<EasyQueryData> selectAll(){
        return easyQueryClient.queryable(EasyQueryData.class).toList();
    }
}
