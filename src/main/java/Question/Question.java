package Question;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class Question {

    private static ConcurrentHashMap<Integer, Question> allQuestions;

    static {
        allQuestions = new ConcurrentHashMap<>();
    }

    private int id;
    private int mark;
    private String content;
    private String answer;
    private String type;
    private String[] choices;

    public Question ( int id, String type ) {
        this.id = id;
        this.type = type;
    }

    public static void addQuestion ( Question question ) {
        allQuestions.put( question.getId(), question );
    }


    public static ConcurrentHashMap<Integer, Question> getAllQuestions () {
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

    public void setChoices ( String[] cs ) {
        this.choices = cs;
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
        question.put( "type", type );
        if ( type.equals( "choice" ) ) {
            question.put( "choiceA", choices[ 0 ] );
            question.put( "choiceB", choices[ 1 ] );
            question.put( "choiceC", choices[ 2 ] );
            question.put( "choiceD", choices[ 3 ] );
        } else {
            question.put( "choiceA", "T" );
            question.put( "choiceB", "F" );
        }
        question.put( "answer", answer );
        return question;
    }

}
