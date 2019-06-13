package FileManger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;

public class FileManger {
    private static HashMap<String, String> fileMap;

    static {
        fileMap = new HashMap<>();
        addFile( "test", "/client/resources/files/test.pdf" );
        addFile( "cpu", "/client/resources/files/cpu.vsdx" );
    }

    public static void addFile ( String name, String uri ) {
        fileMap.put( name, uri );
    }

    public static void deleteFile ( String name ) {
        if ( fileMap.containsKey( name ) ) {
            fileMap.remove( name );
        }
    }

    public static JSONObject toJsonList () {
        JSONObject fileLists = new JSONObject( true );
        JSONArray fileArray = new JSONArray();
        for ( String name : fileMap.keySet()
        ) {
            JSONObject jsonObject = new JSONObject( true );
            jsonObject.put( "name", name );
            jsonObject.put( "url", fileMap.get( name ) );
            fileArray.add( jsonObject );
        }

        fileLists.put( "files", fileArray );
        return fileLists;
    }

}
