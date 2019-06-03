package DataBase;

import User.Student;
import User.TA;
import User.Teacher;
import User.User;

import java.sql.*;
import java.util.ArrayList;

public class DataBase {
    private Connection c = null;

    public DataBase () {
        try {
            Class.forName( "org.sqlite.JDBC" );
            c = DriverManager.getConnection( "jdbc:sqlite:SeProject.db" );
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit( 0 );
        }

        try {
            initUserTable( "STUDENT" );
            initUserTable( "TEACHER" );
            initUserTable( "TA" );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public void initUserTable ( String userType ) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + userType +
                " ( UserID      LONG      PRIMARY KEY   NOT NULL," +
                " UserName     VARCHAR(45)  NOT NULL, " +
                " Password        CHAR(16)     NOT NULL, " +
                " Email           VARCHAR(45)  DEFAULT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table: " + userType );
        }

        stmt.close();
    }


    public void close () throws SQLException {
        c.close();
    }

    public void loadUserInfo () {
        ArrayList<User> students = null;
        ArrayList<User> teachers = null;
        ArrayList<User> tas = null;

        try {
            students = selectUserAll( "STUDENT" );
            teachers = selectUserAll( "TEACHER" );
            tas = selectUserAll( "TA" );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }

        for ( User stu : students ) {
            Student.addUser( stu );
        }
        for ( User tea : teachers ) {
            Teacher.addUser( tea );
        }
        for ( User ta : tas ) {
            TA.addUser( ta );
        }
    }

    public int insertInfo ( User user ) throws SQLException {

        ArrayList<User> users = selectInfoByID( user.getId(), user.getUserType() );
        if ( users.size() != 0 ) {
            return -1; // muti primary key
        }

        String sql = "INSERT INTO " + user.getUserType() + " ( UserID, UserName, Password, Email)VALUES( ?,?,?,?)";
        PreparedStatement ps = c.prepareStatement( sql );

        ps.setLong( 1, user.getId() );
        ps.setString( 2, user.getName() );
        ps.setString( 3, user.getPassword() );
        ps.setString( 4, user.getEmail() );
        int count = ps.executeUpdate();
        System.out.println( "Insert into table" + user.getUserType() + " " + count + " records" );

        ps.close();

        return 0;
    }

    public ArrayList<User> selectUserAll ( String userType ) throws SQLException {
        String sql = "SELECT * FROM " + userType;

        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery( sql );
        ArrayList<User> users = new ArrayList<>();

        while ( rs.next() ) {
            users.add( new Student(
                    rs.getLong( "UserID" ),
                    rs.getString( "UserName" ),
                    rs.getString( "Email" ),
                    rs.getString( "Password" )
            ) );
        }

        stmt.close();
        rs.close();
        return users;
    }


    public ArrayList<User> selectInfoByID ( long id, String userType ) throws SQLException {
        String sql = "SELECT * FROM " + userType + " WHERE UserID = ?";
        PreparedStatement ps = c.prepareStatement( sql );

        ps.setLong( 1, id );

        ResultSet rs = ps.executeQuery();

        ArrayList<User> users = new ArrayList<>();

        while ( rs.next() ) {
            users.add( new Student(
                    rs.getLong( "UserID" ),
                    rs.getString( "UserName" ),
                    rs.getString( "Email" ),
                    rs.getString( "Password" )
            ) );
        }

        ps.close();
        rs.close();
        return users;
    }
}
