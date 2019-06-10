package User;

public class Teacher extends User {

    private String classPostion;

    public Teacher ( Long id, String name, String email, String password ) {
        super( id, name, email, password );
    }

    @Override
    public String getUserType () {
        return "TEACHER";
    }

    public String getClassPostion () {
        return classPostion;
    }

    public void setClassPostion ( String postion ) {
        this.classPostion = postion;
    }


}
