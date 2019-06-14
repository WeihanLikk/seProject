package Question;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class Question {

    private static ConcurrentHashMap<Long, Question> allQuestions;

    static {
        allQuestions = new ConcurrentHashMap<>();
    }

    private Long id;
    private int mark;
    private String content;
    private String answer;
    private String type;
    private String[] choices;

    public Question ( Long id, String content, String type, int mark, String answer, String[] choices ) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.answer = answer;
        this.choices = choices;
        this.mark = mark;
    }

    public static void addQuestion ( Question question ) {
        allQuestions.put( question.getId(), question );
    }

    public static String getFuckID () {
        String s = "";
        for ( long id : allQuestions.keySet()
        ) {
            s += " " + id;
        }

        return s;
    }


    public static ConcurrentHashMap<Long, Question> getAllQuestions () {
        return allQuestions;
    }

    public static JSONObject allToJsonObject () {
        JSONObject jsonObject = new JSONObject( true );

        JSONObject choice = new JSONObject( true );
        JSONObject judge = new JSONObject( true );

        int numberChoice = 0;
        int numberJudge = 0;

        JSONArray choices = new JSONArray();

        JSONArray judges = new JSONArray();

        for ( Long key : allQuestions.keySet()
        ) {
            Question question = allQuestions.get( key );
            if ( question.getType().equals( "choice" ) ) {
                numberChoice++;
                choices.add( question.toJsonObject() );

            } else {
                numberJudge++;
                judges.add( question.toJsonObject() );
            }
        }
        choice.put( "number", numberChoice );
        choice.put( "problems", choices );

        judge.put( "number", numberJudge );
        judge.put( "problems", judges );


        jsonObject.put( "choice", choice );
        jsonObject.put( "judge", judge );
        return jsonObject;
    }

    public static Question getQuestion ( long id ) {
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

    public String getType () {
        return this.type;
    }

    public void setChoices ( String[] cs ) {
        this.choices = cs;
    }

    public Long getId () {
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
        question.put( "id", id );
        question.put( "content", content );
        if ( this.type.equals( "choice" ) ) {
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
