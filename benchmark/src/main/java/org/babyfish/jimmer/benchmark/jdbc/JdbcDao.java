package org.babyfish.jimmer.benchmark.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcDao {

    private final DataSource dataSource;

    public JdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<JdbcData> findAllByColumnIndex() throws SQLException {
        List<JdbcData> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("select * from data")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JdbcData data = new JdbcData();
                        data.setId(rs.getLong(1));
                        data.setValue1(rs.getInt(2));
                        data.setValue2(rs.getInt(3));
                        data.setValue3(rs.getInt(4));
                        data.setValue4(rs.getInt(5));
                        data.setValue5(rs.getInt(6));
                        data.setValue6(rs.getInt(7));
                        data.setValue7(rs.getInt(8));
                        data.setValue8(rs.getInt(9));
                        data.setValue9(rs.getInt(10));
                    }
                }
            }
        }
        return list;
    }

    public List<JdbcData> findAllByColumnName() throws SQLException {
        List<JdbcData> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("select * from data")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JdbcData data = new JdbcData();
                        data.setId(rs.getLong("ID"));
                        data.setValue1(rs.getInt("VALUE_1"));
                        data.setValue2(rs.getInt("VALUE_2"));
                        data.setValue3(rs.getInt("VALUE_3"));
                        data.setValue4(rs.getInt("VALUE_4"));
                        data.setValue5(rs.getInt("VALUE_5"));
                        data.setValue6(rs.getInt("VALUE_6"));
                        data.setValue7(rs.getInt("VALUE_7"));
                        data.setValue8(rs.getInt("VALUE_8"));
                        data.setValue9(rs.getInt("VALUE_9"));
                    }
                }
            }
        }
        return list;
    }
}
