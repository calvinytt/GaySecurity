package server;

public class Account
{
    private String id;
    private String password;

    public Account(String id, String password)
    {
        this.id = id;
        this.password = password;
    }

    public String GetId()
    {
        return id;
    }

    public String GetPassword()
    {
        return password;
    }

}