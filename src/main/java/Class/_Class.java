package Class;

import java.util.ArrayList;
import java.util.List;

public class _Class {

    private static List<_Class> classList;

    static {
        classList = new ArrayList<>();
    }

    private long id;
    private String name;
    private String description;
    private String notice;

    public _Class ( long id, String name, String description, String notice ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.notice = notice;
    }

    public static void addClass ( _Class _class ) {
        classList.add( _class );
    }

    public long getId () {
        return id;
    }

    public String getName () {
        return name;
    }

    public String getDescription () {
        return description;
    }

    public String getNotice () {
        return notice;
    }
}
