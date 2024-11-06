package org.babyfish.jimmer.benchmark.mybatis.plus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MyBatisPlusDataBaseMapper extends BaseMapper<MyBatisPlusData> {
}
