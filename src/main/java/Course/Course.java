package Course;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private static List<Course> courseList;

    static {
        courseList = new ArrayList<Course>();
    }

    private long id;
    private String name;
    private String CourseDesc;
    private String CourseNote;

    public Course ( long id, String name, String courseDesc, String courseNote ) {
        this.id = id;
        this.name = name;
        CourseDesc = courseDesc;
        CourseNote = courseNote;
    }

    public static void addCourse ( Course course ) {
        courseList.add( course );
    }

    public static List<Course> getCourseList () {
        return courseList;
    }

    public String getName () {
        return name;
    }

    public String getCourseDesc () {
        return CourseDesc;
    }

    public String getCourseNote () {
        return CourseNote;
    }

    public long getId () {
        return id;
    }

}
