package server;

public class Account
{
    private String id;
    private String password;
    private String mail;
    private String otp;

    public Account(String id, String password, String mail, String otp)
    {
        this.id = id;
        this.password = password;
        this.mail = mail;
        this.otp = otp;
    }

    public String GetId()
    {
        return id;
    }

    public String GetPassword()
    {
        return password;
    }

    public String GetMail()
    {
        return mail;
    }

    public String GetOtp()
    {
        return otp;
    }

}