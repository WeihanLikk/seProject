package User;

public class Teacher extends User {


    public Teacher ( Long id, String name, String email, String password ) {
        super( id, name, email, password );
    }

    @Override
    public String getUserType () {
        return "TEACHER";
    }

}
