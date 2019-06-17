package User;

public class TA extends User {


    public TA ( Long id, String name, String email, String password ) {
        super( id, name, email, password );
    }


    @Override
    public String getUserType () {
        return "TA";
    }


}
