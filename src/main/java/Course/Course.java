package Course;

import User.Student;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private static List<Course> courseList;

    static {
        courseList = new ArrayList<Course>();
    }

    private List<Student> studentList;

    public Course () {
        studentList = new ArrayList<>();
    }

    public static void addCourse ( Course course ) {
        courseList.add( course );
    }

}
