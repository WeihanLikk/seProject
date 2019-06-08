package User;

import Class._Class;

import java.util.ArrayList;

public class Student extends User {



    public Student ( Long id, String name, String email, String password ) {
        super( id, name, email, password );
    }


    @Override

    public String getUserType () {
        return "student";
    }
}
