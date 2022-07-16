package org.babyfish.jimmer.benchmark.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DataMapper {

    @Select("SELECT * FROM DATA")
    @Results({
            @Result(id = true, property = "id", column = "ID"),
            @Result(property = "value1", column = "VALUE_1"),
            @Result(property = "value2", column = "VALUE_2"),
            @Result(property = "value3", column = "VALUE_3"),
            @Result(property = "value4", column = "VALUE_4"),
            @Result(property = "value5", column = "VALUE_5"),
            @Result(property = "value6", column = "VALUE_6"),
            @Result(property = "value7", column = "VALUE_7"),
            @Result(property = "value8", column = "VALUE_8"),
            @Result(property = "value9", column = "VALUE_9")
    })
    List<MyBatisData> findAll();
}
