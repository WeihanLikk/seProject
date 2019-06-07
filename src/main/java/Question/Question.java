package Question;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;

public class Question {

    private static HashMap<Integer, Question> allQuestions;

    static {
        allQuestions = new HashMap<>();
    }

    private int id;
    private int mark;
    private String content;
    private String answer;

    public Question ( int id ) {
        this.id = id;
    }

    public static void addQuestion ( Question question ) {
        allQuestions.put( question.getId(), question );
    }


    public static HashMap<Integer, Question> getAllQuestions () {
        return allQuestions;
    }

    public static JSONObject allToJsonObject () {
        JSONObject jsonObject = new JSONObject( true );
        JSONArray jsonArray = new JSONArray();

        for ( int key : allQuestions.keySet()
        ) {
            jsonArray.add( allQuestions.get( key ).toJsonObject() );
        }

        jsonObject.put( "questions", jsonArray );
        return jsonObject;
    }

    public static Question getQuestion ( int id ) {
        if ( allQuestions.containsKey( id ) ) {
            return allQuestions.get( id );
        }
        return null;
    }

    public int getMark () {
        return mark;
    }

    public void setMark ( int mark ) {
        this.mark = mark;
    }

    public int getId () {
        return id;
    }

    public void setContent ( String content ) {
        this.content = content;
    }

    public void setAnswer ( String answer ) {
        this.answer = answer;
    }

    public int judge ( String answer ) {
        if ( answer.equals( this.answer ) ) {
            return mark;
        } else {
            return 0;
        }
    }

    public JSONObject toJsonObject () {
        JSONObject question = new JSONObject( true );
        question.put( "content", content );
        question.put( "answer", answer );
        return question;
    }

}
