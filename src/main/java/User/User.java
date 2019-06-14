package User;

import Class._Class;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private static ConcurrentHashMap<Long, User> userHashMap;

    static {
        userHashMap = new ConcurrentHashMap<Long, User>();
    }

    private String classPostion;
    private long hwGetTarget;
    private ArrayList<_Class> classArrayList;
    private long id;
    private String name;
    private String email;
    private String password;
    private boolean isActive = false;

    public User ( Long id, String name, String email, String password ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public static User findUser ( long id ) {
        return userHashMap.get( id );
    }

    public static void addUser ( User user ) {
        userHashMap.put( user.getId(), user );
    }

    public String getClassPosition () {
        return classPostion;
    }

    public void setClassPosition ( String postion ) {
        this.classPostion = postion;
    }

    public long getHwGetTarget () {
        return hwGetTarget;
    }

    public void setHwGetTarget ( long id ) {
        this.hwGetTarget = id;
    }

    public void addClasses ( ArrayList<_Class> classes ) {
        classArrayList = classes;
    }

    public ArrayList<_Class> getClassArrayList () {
        return classArrayList;
    }

    public String getUserType () {
        return "User";
    }

    public Long getId () {
        return this.id;
    }

    public void setId ( long id ) {
        this.id = id;
    }

    public void setActive ( boolean active ) {
        isActive = active;
    }

    public String getEmail () {
        return email;
    }

    public void setEmail ( String email ) {
        this.email = email;
    }

    public String getPassword () {
        return password;
    }

    public void setPassword ( String password ) {
        this.password = password;
    }

    public String getName () {
        return name;
    }

    public void setName ( String name ) {
        this.name = name;
    }

}
