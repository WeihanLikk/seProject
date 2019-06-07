package User;

import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;

public class User {
    private static HashMap<Long, User> userHashMap;
    private static HashMap<ChannelHandlerContext, User> channelHashMap;

    static {
        userHashMap = new HashMap<Long, User>();
        channelHashMap = new HashMap<ChannelHandlerContext, User>();
    }

    private long id;
    private String name;
    private String email;
    private String password;
    private boolean isActive = false;

    public User ( Long id, String name, String email, String password ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public static User findUser ( long id ) {
        return userHashMap.get( id );
    }

    public static void addUser ( User user ) {
        userHashMap.put( user.getId(), user );
    }

    public static void bindUser ( ChannelHandlerContext ctx, User user ) {
        channelHashMap.put( ctx, user );
    }


    public String getUserType () {
        return "User";
    }

    public long getId () {
        return this.id;
    }

    public void setId ( long id ) {
        this.id = id;
    }

    public void setActive ( boolean active ) {
        isActive = active;
    }

    public String getEmail () {
        return email;
    }

    public void setEmail ( String email ) {
        this.email = email;
    }

    public String getPassword () {
        return password;
    }

    public void setPassword ( String password ) {
        this.password = password;
    }

    public String getName () {
        return name;
    }

    public void setName ( String name ) {
        this.name = name;
    }

}
