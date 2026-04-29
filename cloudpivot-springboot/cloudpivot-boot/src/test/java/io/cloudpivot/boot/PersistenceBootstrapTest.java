package io.cloudpivot.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersistenceBootstrapTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldBootstrapSeedDataForPlatformAndMetadata() throws Exception {
        assertNotNull(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            assertEquals(3, queryCount(statement, "select count(*) from iam_user"));
            assertEquals(3, queryCount(statement, "select count(*) from iam_role"));
            assertEquals(3, queryCount(statement, "select count(*) from sys_announcement"));
            assertEquals(3, queryCount(statement, "select count(*) from meta_app"));
            assertEquals(1, queryCount(statement, "select count(*) from meta_object"));
            assertEquals(3, queryCount(statement, "select count(*) from meta_object_field"));
            assertEquals(1, queryCount(statement, "select count(*) from meta_page"));
            assertEquals(3, queryCount(statement, "select count(*) from meta_component"));
            assertEquals(1, queryCount(statement, "select count(*) from meta_publish_version"));
            assertEquals(3, queryCount(statement, "select count(*) from plugin_registry"));
        }
    }

    private long queryCount(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
