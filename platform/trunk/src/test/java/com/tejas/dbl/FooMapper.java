package com.tejas.dbl;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.session.RowBounds;

public interface FooMapper
{

    static final String COLUMN_MAPPING = "foo_id as fooId, foo_string as fooString, " +
            "foo_blob as fooBlob, " +
            "foo_clob as fooClob, " +
            "foo_date as fooDate," +
            "foo_time as fooTime," +
            "foo_bigint as fooBigInt," +
            "foo_bigdecimal as fooBigDecimal," +
            "foo_double as fooDouble," +
            "foo_boolean as fooBoolean";

    @Select("select count(*) from FOO")
    long countEntries();

    @Update("CREATE TABLE if not exists FOO (\n" +
            "  foo_id int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  foo_string varchar(45) NOT NULL,\n" +
            "  foo_blob longblob,\n" +
            "  foo_clob longtext,\n" +
            "  foo_date date DEFAULT NULL,\n" +
            "  foo_time datetime DEFAULT NULL,\n" +
            "  foo_bigint bigint(20) DEFAULT NULL,\n" +
            "  foo_bigdecimal decimal(20,2) DEFAULT NULL,\n" +
            "  foo_double double DEFAULT NULL,\n" +
            "  foo_boolean tinyint(1) NOT NULL,\n" +
            "  PRIMARY KEY (foo_id),\n" +
            "  UNIQUE KEY foo_string_UNIQUE (foo_string)\n" +
            ") ENGINE=InnoDB")
    void createTable();

    @Update("delete from FOO")
    int deleteAllData();

    @Update("delete from FOO where foo_id = #{fooId}")
    int deleteFooById(long fooId);

    @Update("delete from FOO where foo_string = #{fooString}")
    int deleteFooByString(String fooString);

    @Update("drop table if exists FOO")
    int dropTable();

    @Options(useGeneratedKeys = true, keyProperty = "fooId")
    @Insert("Insert into FOO(foo_string, foo_blob, foo_clob, foo_date, foo_time, foo_bigint, foo_bigdecimal, foo_double, foo_boolean) " +
            "values (#{fooString}, #{fooBlob}, #{fooClob}, #{fooDate}, #{fooTime}, #{fooBigInt}, #{fooBigDecimal}, #{fooDouble}, #{fooBoolean})")
    int insertFoo(Foo param);

    @Select("select " + COLUMN_MAPPING + " from FOO order by foo_id")
    List<Foo> selectAllFoos();

    @Select("select " + COLUMN_MAPPING + " from FOO where foo_id = #{fooId}")
    Foo selectByFooId(long fooId);

    @Select("select " + COLUMN_MAPPING + " from FOO where foo_string = #{fooString}")
    Foo selectByFooString(String fooString);

    @Select("select " + COLUMN_MAPPING + " from FOO order by foo_id")
    List<Foo> selectSomeFoos(RowBounds bounds);

    @Update("truncate FOO")
    int truncateTable();

}
