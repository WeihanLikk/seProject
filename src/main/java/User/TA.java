package User;

import Class._Class;

import java.util.ArrayList;

public class TA extends User {

    private ArrayList<_Class> classArrayList;

    public TA ( Long id, String name, String email, String password ) {
        super( id, name, email, password );
    }


    @Override
    public String getUserType () {
        return "TA";
    }

    public void addClasses ( ArrayList<_Class> classArrayList ) {
        this.classArrayList = classArrayList;
    }
}
