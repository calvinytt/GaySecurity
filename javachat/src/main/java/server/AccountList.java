package server;

import java.util.ArrayList;

public class AccountList
{
    private ArrayList<Account> list = new ArrayList<Account>();

    // Size of list
    public boolean IsEmpty()
    {
        return list.size() == 0;
    }

    // Add account when initializing
    public void Add(String id, String password, String mail, String otp)
    {
        list.add(new Account(id, password, mail, otp));
    }

    // Clear for initialize again when db edited
    public void clear()
    {
        list.clear();
    }

    // Check login success
    public boolean Contain(String id, String password)
    {
        for (Account account : list)
        {
            if (account.GetId().equals(id) && account.GetPassword().equals(password))
            {
                return true;
            }
        }

        return false;
    }

    // Get mail by id after login success
    public String GetMail(String id)
    {
        for (Account account : list)
        {
            if (account.GetId().equals(id))
            {
                return account.GetMail();
            }
        }

        System.out.println("No this id");
        return null;
    }

    // Debug
    public void PrintList()
    {
        // If empty
        if (list.size() == 0)
        {
            System.out.println("Usage: account list not initialized");
            return;
        }
    
        System.out.println("------------------------------Account List-------------------------");
        System.out.println("Amount of account: " + list.size());
        System.out.println("------------------------------");
        System.out.println("--id--\t--password--\t--mail--\t--otp");
    
        for (Account account : list)
        {
            System.out.println(account.GetId() + "\t" + account.GetPassword() + "\t" + account.GetMail() + "\t" + account.GetOtp());
        }
    
        System.out.println("------------------------------end Account List-------------------------");
    }

}