package Class;

import Homework.Homework;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class _Class {

    private static ConcurrentHashMap<Long, _Class> classMap;

    static {
        classMap = new ConcurrentHashMap<>();
    }

    private List<Homework> homeworkList;
    private long id;
    private String name;
    private String description;

    public _Class ( long id, String name, String description ) {
        this.id = id;
        this.name = name;
        this.description = description;
        homeworkList = new ArrayList<>();
    }

    public static void addClass ( _Class _class ) {
        classMap.put( _class.getId(), _class );
    }

    public static _Class get_Class ( long id ) {
        if ( classMap.containsKey( id ) ) {
            return classMap.get( id );
        }
        return null;
    }

    public void addHomeWork ( Homework homework ) {
        homeworkList.add( homework );
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

    public JSONObject toHomeworkList () {
        JSONObject jsonObject = new JSONObject( true );
        JSONArray jsonArray = new JSONArray();

        for ( Homework ho : homeworkList
        ) {
            JSONObject jsonObject1 = new JSONObject( true );
            jsonObject1.put( "id", ho.getId() );
            jsonObject1.put( "name", ho.getName() );
            jsonArray.add( jsonObject1 );
        }

        jsonObject.put( "Homework List", jsonArray );
        return jsonObject;
    }
}
