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
    public void Add(String id, String password)
    {
        list.add(new Account(id, password));
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
        System.out.println("--id--\t--password--");
    
        for (Account account : list)
        {
            System.out.println(account.GetId() + "\t" + account.GetPassword());
        }
    
        System.out.println("------------------------------end Account List-------------------------");
    }

}