package connections;

import connections.query.resultAttribute.Attribute;
import exceptions.ConnectionException;
import connections.query.queryAttribute.QueryAttribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DbConnection {

    protected Connection connection;
    protected ResultSet resultSet;
    protected PreparedStatement statement;

    private static final String FILENAME = "src/Password.txt";
    private static final String DRIVERCLASS = "com.mysql.cj.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/db-escaperoom";
    private static final String USER = "root";
    private String password;

    public DbConnection() {
        try {
            this.password = readPassword();
        } catch (IOException e) {
            System.err.println("Error. Could not read the file.");
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void callCreate(String query, List<QueryAttribute> queryAttributes) {
        try {
            this.createConnection();
            this.createStatementWithQuery(query);
            this.addAttributesToStatement(queryAttributes);
            this.executeCreate();
        } catch (ConnectionException e) {
            System.out.println(e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public List<HashMap<String, Object>> callQuery(String query, List<QueryAttribute> queryAttributes, List<Attribute> attributes) {

        List<HashMap<String, Object>> list = new ArrayList<>();

        try {
            this.createConnection();
            this.createStatementWithQuery(query);
            this.addAttributesToStatement(queryAttributes);
            this.resultSet = this.executeQuery();

            while(this.resultSet.next()) {
                HashMap<String, Object> hasMap = new HashMap<>();
                for (Attribute attribute: attributes) {
                    Object obj = null;
                    switch (attribute.getType()) {
                        case STRING -> obj = this.resultSet.getString(attribute.getName());
                        case INT -> obj = this.resultSet.getInt(attribute.getName());
                        case DOUBLE -> obj = this.resultSet.getDouble(attribute.getName());
                    }
                    hasMap.put(attribute.getName(), obj);
                }
                list.add(hasMap);
            }
        } catch (ConnectionException | SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            closeConnection();
        }

        return list;
    }

    private void executeCreate() throws ConnectionException {
        try {
            this.statement.executeUpdate();
        } catch (SQLException e) {
            throw new ConnectionException("Error executing statement to create an object.");
        }
    }

    public ResultSet executeQuery() throws ConnectionException {
        try {
            this.resultSet = this.statement.executeQuery();
            return this.resultSet;
        } catch (SQLException e) {
            throw new ConnectionException("Error executing statement to query into de ddbb.");
        }
    }

    private void createConnection() throws ConnectionException {
        try {
            Class.forName(DRIVERCLASS);
            this.connection = DriverManager.getConnection(URL, USER, this.password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ConnectionException("Error while attempting connection to the database.");
        }
    }

    private void createStatementWithQuery(String query) throws ConnectionException {
        try {
            this.statement = connection.prepareStatement(query);
        } catch (SQLException e) {
            throw new ConnectionException("Error while creating statement from query " + query);
        }
    }

    private void addAttributesToStatement(List<QueryAttribute> attributes) throws ConnectionException {
        for (QueryAttribute attribute: attributes) {
            try {
                this.statement = attribute.addToStatement(this.statement);
            } catch (ConnectionException e) {
                throw new ConnectionException(e.getMessage());
            }
        }
    }

    private void closeResultSet() {
        try {
            resultSet.close();
        } catch (SQLException ex) {
            System.err.println("Error. Couldn't close resultSet.");
        }
    }

    private void closeStatement() {
        try {
            statement.close();
        } catch (SQLException ex) {
            System.err.println("Error. Couldn't close statement.");
        }
    }

    private void closeConnection() {
        try {
            if (resultSet != null) {
                closeResultSet();
            }
            if (statement != null) {
                closeStatement();
            }
            connection.close();
        } catch (SQLException e) {
            System.err.println("Error. Couldn't close the connection properly.");
        }
    }

    private static String readPassword() throws IOException {
        Path fileName = Path.of(DbConnection.FILENAME);
        return Files.readString(fileName);
    }
}
