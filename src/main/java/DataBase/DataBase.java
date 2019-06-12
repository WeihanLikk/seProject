package DataBase;

import Class._Class;
import Course.Course;
import Question.Question;
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

//        try {
//            initUserTable( "STUDENT" );
//            initUserTable( "TEACHER" );
//            initUserTable( "TA" );
//            initCourseTable();
//            initClassTable();
//            intiQuestionTable();
//            intiHomeWorkTable();
//            initStuClassTable();
//
//        } catch ( SQLException e ) {
//            e.printStackTrace();
//        }
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

        try {
            initUserTable( "STUDENT" );
            initUserTable( "TEACHER" );
            initUserTable( "TA" );
            initCourseTable();
            initClassTable();
            intiQuestionTable();
            intiHomeWorkTable();
            initUserClassTable( "STUCLASS" );
            initUserClassTable( "TEACLASS" );
            initUserClassTable( "TACLASS" );

        } catch ( SQLException e ) {
            e.printStackTrace();
        }

    }

    public static void loadInfo () {

        try {
            ArrayList<User> students = selectUserAll( "STUDENT" );
            ArrayList<User> teachers = selectUserAll( "TEACHER" );
            ArrayList<User> tas = selectUserAll( "TA" );
            ArrayList<Course> courses = selectCourseAll();
            ArrayList<_Class> classes = selectClassAll();
            ArrayList<Question> questions = selectQuestionAll();

            for ( User stu : students ) {
                Student student = (Student) stu;
                ArrayList<_Class> classArrayList = selectUserClassAll( "STUCLASS", student.getId() );
                student.addClasses( classArrayList );
                User.addUser( student );
            }
            for ( User tea : teachers ) {
                Teacher teacher = (Teacher) tea;
                ArrayList<_Class> classArrayList = selectUserClassAll( "TEACLASS", teacher.getId() );
                teacher.addClasses( classArrayList );
                User.addUser( teacher );
            }
            for ( User ta : tas ) {
                TA tagg = (TA) ta;
                ArrayList<_Class> classArrayList = selectUserClassAll( "TACLASS", tagg.getId() );
                tagg.addClasses( classArrayList );
                User.addUser( tagg );
            }
            for ( Course course : courses ) {
                Course.addCourse( course );
            }
            for ( _Class _class : classes ) {
                _Class.addClass( _class );
            }

            for ( Question ques : questions
            ) {
                Question.addQuestion( ques );
            }

            //connection.close();

        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public static long selectClassIdByUserAndClassName ( Long UserId, String className, String userType ) throws SQLException {
        String sql = "SELECT ClassID FROM " + userType + " WHERE UserID = ? AND ClassName = ?";
        PreparedStatement ps = connection.prepareStatement( sql );

        ps.setLong( 1, UserId );
        ps.setString( 2, className );

        ResultSet rs = ps.executeQuery();

        long classId = 0;

        while ( rs.next() ) {
            classId = rs.getLong( "ClassID" );
        }

        return classId;
    }

    public static ArrayList<User> selectUserAll ( String userType ) throws SQLException {
        String sql = "SELECT * FROM " + userType;

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery( sql );
        ArrayList<User> users = new ArrayList<>();

        while ( rs.next() ) {
            if ( userType.equals( "STUDENT" ) ) {
                users.add( new Student(
                        rs.getLong( "UserID" ),
                        rs.getString( "UserName" ),
                        rs.getString( "Email" ),
                        rs.getString( "Password" )
                ) );
            } else if ( userType.equals( "TEACHER" ) ) {
                users.add( new Teacher(
                        rs.getLong( "UserID" ),
                        rs.getString( "UserName" ),
                        rs.getString( "Email" ),
                        rs.getString( "Password" )
                ) );
            } else if ( userType.equals( "TA" ) ) {
                users.add( new TA(
                        rs.getLong( "UserID" ),
                        rs.getString( "UserName" ),
                        rs.getString( "Email" ),
                        rs.getString( "Password" )
                ) );
            }
        }

        stmt.close();
        rs.close();
        return users;
    }

    public static ArrayList<_Class> selectUserClassAll ( String name, long id ) throws SQLException {
        String sql = "SELECT * FROM " + name + " WHERE UserID = ?";

        PreparedStatement ps = connection.prepareStatement( sql );

        ps.setLong( 1, id );

        ResultSet rs = ps.executeQuery();

        ArrayList<_Class> classes = new ArrayList<>();

        while ( rs.next() ) {
            classes.add( new _Class(
                    rs.getLong( "ClassID" ),
                    rs.getString( "ClassName" ),
                    rs.getString( "ClassDesc" )
            ) );
        }

        ps.close();
        rs.close();
        return classes;
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

    public static ArrayList<Question> selectQuestionAll () throws SQLException {
        String sql = "SELECT * FROM QUESTION";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery( sql );
        ArrayList<Question> questions = new ArrayList<>();

        while ( rs.next() ) {

            String[] choices = new String[ 4 ];
            choices[ 0 ] = rs.getString( "ChoiceA" );
            choices[ 1 ] = rs.getString( "ChoiceB" );
            choices[ 2 ] = rs.getString( "ChoiceC" );
            choices[ 3 ] = rs.getString( "ChoiceD" );

            questions.add( new Question(
                    rs.getLong( "QuestionId" ),
                    rs.getString( "Content" ),
                    rs.getString( "TYPE" ),
                    rs.getInt( "MARK" ),
                    rs.getString( "Answer" ),
                    choices
            ) );
        }

        stmt.close();
        rs.close();
        return questions;
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
                    rs.getString( "ClassDesc" )
            ) );
        }

        stmt.close();
        rs.close();
        return classes;
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

    public void initUserClassTable ( String name ) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + name +
                " ( UserID      LONG      NOT NULL," +
                "   ClassID     LONG      NOT NULL, " +
                "   ClassName   VARCHAR(50) NOT NULL," +
                "   ClassDesc   VARCHAR(100)     DEFAULT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table: " + name );
        }

        stmt.close();
    }

    public void intiQuestionTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS QUESTION" +
                " ( QuestionId      LONG      PRIMARY KEY   NOT NULL," +
                " Content     VARCHAR(100)  NOT NULL, " +
                " Answer        CHAR(5)     NOT NULL, " +
                " TYPE          CHAR(10)    NOT NULL," +
                " MARK          INT         NOT NULL," +
                " ChoiceA        VARCHAR(50)  NOT NULL," +
                " ChoiceB        VARCHAR(50)  NOT NULL," +
                " ChoiceC        VARCHAR(50)  NOT NULL," +
                " ChoiceD        VARCHAR(50)  NOT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table question" );
        }

        stmt.close();
    }

    public void initClassTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS CLASS" +
                " ( ClassId      LONG      PRIMARY KEY   NOT NULL," +
                " ClassName     VARCHAR(50)  NOT NULL, " +
                " ClassDesc       VARCHAR(100) DEFAULT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table class" );
        }

        stmt.close();
    }

    public void intiHomeWorkTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS HOMEWORK" +
                " ( HomeWorkId      LONG      PRIMARY KEY   NOT NULL," +
                " QuestionList     VARCHAR(100)  NOT NULL, " +
                " CreateDate        VARCHAR(20)  DEFAULT NULL, " +
                " DeadLine        VARCHAR(20)  DEFAULT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table homework" );
        }

        stmt.close();
    }

    public void initCourseTable () throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS COURSE" +
                " ( CourseId      LONG      PRIMARY KEY   NOT NULL," +
                " CourseName     VARCHAR(45)  NOT NULL, " +
                " CourseDesc       VARCHAR(200) DEFAULT NULL, " +
                " CourseNote       VARCHAR(200)  DEFAULT NULL)";

        Statement stmt = c.createStatement();

        if ( stmt.execute( sql ) ) {
            System.out.println( "create table course" );
        }

        stmt.close();
    }

    public void close () throws SQLException {
        c.close();
        connection.close();
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

    public int insertClassInfo ( _Class _class ) throws SQLException {
        ArrayList<_Class> classes = selectClassInfoByID( _class.getId() );
        if ( classes.size() != 0 ) {
            return -1;
        }

        String sql = "INSERT INTO CLASS (ClassID, ClassName, ClassDesc)VALUES(?,?,?)";
        PreparedStatement ps = c.prepareStatement( sql );
        ps.setLong( 1, _class.getId() );
        ps.setString( 2, _class.getName() );
        ps.setString( 3, _class.getDescription() );

        int count = ps.executeUpdate();
        System.out.println( "Insert into table CLASS " + count + " records" );

        ps.close();

        return 0;
    }

    public ArrayList<_Class> selectClassInfoByID ( long id ) throws SQLException {
        String sql = "SELECT * FROM CLASS WHERE ClassID = ?";
        PreparedStatement ps = c.prepareStatement( sql );

        ps.setLong( 1, id );
        ResultSet rs = ps.executeQuery();

        ArrayList<_Class> classes = new ArrayList<>();

        while ( rs.next() ) {
            classes.add( new _Class(
                    rs.getLong( "ClassID" ),
                    rs.getString( "ClassName" ),
                    rs.getString( "ClassDesc" )
            ) );
        }

        ps.close();
        rs.close();
        return classes;
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
