package Homework;

import Question.Question;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Homework {
    private static ConcurrentHashMap<Long, Homework> homeworkList;

    static {
        homeworkList = new ConcurrentHashMap<>();
    }

    private long id;
    private String name;
    private List<Question> questionList;
    private int type;
    private int totalMarks;
    private Data createData, deadLine;

    public Homework ( int id, String name ) {
        this.id = id;
        this.name = name;
        questionList = new ArrayList<>();
        type = -1;
    }

    public static void addHomework ( Homework homework ) {
        homeworkList.put( homework.getId(), homework );
    }

    public static int geneId () {
        return homeworkList.size();
    }


    public long getId () {
        return id;
    }

    public void setId ( int id ) {
        this.id = id;
    }

    public String getName () {
        return this.name;
    }

    public void setCreateData ( Data createData ) {
        this.createData = createData;
    }

    public void setDeadLine ( Data deadLine ) {
        this.deadLine = deadLine;
    }

    public void addQuestion ( Question question ) {
        questionList.add( question );
    }

    public void setTotalMarks () {
        totalMarks = 0;
        for ( Question que :
                questionList ) {
            totalMarks += que.getMark();
        }
    }

    public JSONObject toJsonObject () {
        JSONObject jsonObject = new JSONObject( true );
        JSONArray jsonArray = new JSONArray();

        for ( Question que : questionList
        ) {
            jsonArray.add( que.toJsonObject() );
        }

        jsonObject.put( "id", this.id );
        jsonObject.put( "homework", jsonArray );

        return jsonObject;
    }


}
