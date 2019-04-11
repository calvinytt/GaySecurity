package server;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DatabaseConnect
{
    // Connect server
    public boolean InitializeSDK()
    {
        try {
            FileInputStream serviceAccount = new FileInputStream("java-based-chatting-system-firebase-adminsdk-t0lnn-6dfbe94fdd.json");

            Map<String, Object> auth = new HashMap<String, Object>();
            auth.put("uid", "gaysecurity");  // uid

            FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://java-based-chatting-system.firebaseio.com")
            .setDatabaseAuthVariableOverride(auth) // set uid
            .build();

            FirebaseApp.initializeApp(options);

            System.out.println("Initialize SDK success");

            // Success
            return true;
        } catch (Exception e) {
            e.printStackTrace();

            // Fail
            return false;
        }
    }

    // Get account list by the object to this function
    public void InitializeAccountList(AccountList accounts)
    {
        // The app only has access as defined in the Security Rules
        DatabaseReference ref = FirebaseDatabase
        .getInstance()
        .getReference().child("account");

        System.out.println("Getting account list");

        // Attach a listener to read the data at our posts reference
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // If no account or wrong path
                if (!dataSnapshot.exists())
                {
                    System.out.println("Account not found");
                    return;
                }
                
                // Read/Re-read account list
                // int accountCount = (int) dataSnapshot.getChildrenCount();
                // accounts = new Account[accountCount]; // create temp list
                if (!accounts.IsEmpty())
                {
                    System.out.println("Re-initialize account list");
                    accounts.clear();
                }  

                // int index = 0;
                for (DataSnapshot dataSnap : dataSnapshot.getChildren()) {
                    String id = (String) dataSnap.child("id").getValue();
                    String password = (String) dataSnap.child("password").getValue();
                    // accounts[index] = new Account(id, password); // Store to list
                    accounts.Add(id, password);
                    // index += 1;
                }

                System.out.println("Initialize account list success");

                accounts.PrintList();   // Debug
            }
        
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }
}