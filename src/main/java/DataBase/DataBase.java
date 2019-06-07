package DataBase;

import Class._Class;
import Course.Course;
import User.Student;
import User.TA;
import User.Teacher;
import User.User;

import java.sql.*;
import java.util.ArrayList;

public class DataBase {
    private static Connection connection = null;

    static {
        try {
            Class.forName( "org.sqlite.JDBC" );
            connection = DriverManager.getConnection( "jdbc:sqlite:SeProject.db" );
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit( 0 );
        }

        try {
            initUserTable( "STUDENT" );
            initUserTable( "TEACHER" );
            initUserTable( "TA" );
            initCourseTable();
            initClassTable();
            intiQuestionTable();
            intiHomeWorkTable();

        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    private Connection c = null;

    public DataBase () {
        try {
            Class.forName( "org.sqlite.JDBC" );
            c = DriverManager.getConnection( "jdbc:sqlite:SeProject.db" );
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit( 0 );
        }


    }

    public static void loadInfo () throws SQLException {


        ArrayList<User> students = null;
        ArrayList<User> teachers = null;
        ArrayList<User> tas = null;
        ArrayList<Course> courses = null;
        ArrayList<_Class> classes = null;

        try {
            students = selectUserAll( "STUDENT" );
            teachers = selectUserAll( "TEACHER" );
            tas = selectUserAll( "TA" );
            courses = selectCourseAll();
            classes = selectClassAll();
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
        for ( Course course : courses ) {
            Course.addCourse( course );
        }
        for ( _Class _class : classes ) {
            _Class.addClass( _class );
        }

        connection.close();
    }

    public static ArrayList<User> selectUserAll ( String userType ) throws SQLException {
        String sql = "SELECT * FROM " + userType;

        Statement stmt = connection.createStatement();
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

    public static ArrayList<Course> selectCourseAll () throws SQLException {
        String sql = "SELECT * FROM COURSE";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery( sql );
        ArrayList<Course> courses = new ArrayList<>();

        while ( rs.next() ) {
            courses.add( new Course(
                    rs.getLong( "CourseId" ),
                    rs.getString( "CourseName" ),
                    rs.getString( "CourseDesc" ),
                    rs.getString( "CourseNote" )
            ) );
        }

        stmt.close();
        rs.close();
        return courses;
    }

    public static ArrayList<_Class> selectClassAll () throws SQLException {
        String sql = "SELECT * FROM CLASS";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery( sql );
        ArrayList<_Class> classes = new ArrayList<>();

        while ( rs.next() ) {
            classes.add( new _Class(
                    rs.getLong( "ClassId" ),
                    rs.getString( "ClassName" ),
                    rs.getString( "ClassDesc" ),
                    rs.getString( "ClassNote" )
            ) );
        }

        stmt.close();
        rs.close();
        return classes;
    }

    public static void initUserTable ( String userType ) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + userType +
                " ( UserID      LONG      PRIMARY KEY   NOT NULL," +
                " UserName     VARCHAR(45)  NOT NULL, " +
                " Password        CHAR(16)     NOT NULL, " +
                " Email           VARCHAR(45)  DEFAULT NULL)";

        Statement stmt = connection.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table: " + userType );
        }

        stmt.close();
    }

    public static void intiQuestionTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS QUESTION" +
                " ( QuestionId      LONG      PRIMARY KEY   NOT NULL," +
                " Content     VARCHAR(100)  NOT NULL, " +
                " Answer        VARCHAR(50)     NOT NULL, " +
                " Choice        VARCHAR(50)  NOT NULL)";

        Statement stmt = connection.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table question" );
        }

        stmt.close();
    }

    public static void intiHomeWorkTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS HOMEWORK" +
                " ( HomeWorkId      LONG      PRIMARY KEY   NOT NULL," +
                " QuestionList     VARCHAR(100)  NOT NULL, " +
                " CreateDate        VARCHAR(20)  DEFAULT NULL, " +
                " DeadLine        VARCHAR(20)  DEFAULT NULL)";

        Statement stmt = connection.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table homework" );
        }

        stmt.close();
    }


    public static void initClassTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS CLASS" +
                " ( ClassId      LONG      PRIMARY KEY   NOT NULL," +
                " CourseId        LONG      NOT NULL," +
                " ClassName     VARCHAR(45)  NOT NULL, " +
                " ClassDesc       VARCHAR(200) DEFAULT NULL, " +
                " ClassNote       VARCHAR(200)  DEFAULT NULL, " +
                " FOREIGN KEY(CourseId) REFERENCES COURSE(CourseId)   )";

        Statement stmt = connection.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table class" );
        }

        stmt.close();
    }

    public static void initCourseTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS COURSE" +
                " ( CourseId      LONG      PRIMARY KEY   NOT NULL," +
                " CourseName     VARCHAR(45)  NOT NULL, " +
                " CourseDesc       VARCHAR(200) DEFAULT NULL, " +
                " CourseNote       VARCHAR(200)  DEFAULT NULL)";

        Statement stmt = connection.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table course" );
        }

        stmt.close();
    }

    public void close () throws SQLException {
        c.close();
    }

    public int insertUserInfo ( User user ) throws SQLException {

        ArrayList<User> users = selectUserInfoByID( user.getId(), user.getUserType() );
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

    public ArrayList<User> selectUserInfoByID ( long id, String userType ) throws SQLException {
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
