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

    public static Homework getHomework ( long id ) {
        if ( homeworkList.containsKey( id ) ) {
            return homeworkList.get( id );
        }
        return null;
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

        int numberChoice = 0;
        int numberJudge = 0;

        JSONObject choice = new JSONObject( true );
        JSONObject judge = new JSONObject( true );
        JSONArray choices = new JSONArray();
        JSONArray judges = new JSONArray();

        for ( Question que : questionList
        ) {
            if ( que.getType().equals( "choice" ) ) {
                numberChoice++;
                choices.add( que.toJsonObject() );
            } else {
                numberJudge++;
                judges.add( que.toJsonObject() );
            }
        }

        choice.put( "number", numberChoice );
        choice.put( "problems", choices );

        judge.put( "number", numberJudge );
        judge.put( "problems", judges );

        jsonObject.put( "id", this.id );
        jsonObject.put( "name", this.name );
        JSONObject hk = new JSONObject( true );
        hk.put( "choice", choice );
        hk.put( "judge", judge );
        jsonObject.put( "homework", hk );

        return jsonObject;
    }
}
