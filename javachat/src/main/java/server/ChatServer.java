package server;

import java.net.*;
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

import java.io.*;

public class ChatServer implements Runnable
{  private ChatServerThread clients[] = new ChatServerThread[50];
   private ServerSocket server = null;
   private volatile Thread  thread = null;
   private int clientCount = 0;

   private boolean finishGetAccountList;
   private Account[] accounts;

   public ChatServer(int port)
   {  try
      {           
         // Firebase admin
         if (!InitializeSDK())
         {
            System.out.println("Usage: initialize SDK fail");
            return;
         }

         // Get all account data from db
         InitializeAccountList();

         // Start socket until getting account data finish
         try
         {
            while (!finishGetAccountList)
            {
               Thread.sleep(1000);
            }
         }
         catch(InterruptedException ex)
         {
            Thread.currentThread().interrupt();
         }
         finally
         {
            System.out.println("Binding to port " + port + ", please wait  ...");
            server = new ServerSocket(port);  
            System.out.println("Server started: " + server);
            start();
         }
      }
      catch(IOException ioe)
      {  System.out.println("Can not bind to port " + port + ": " + ioe.getMessage()); }
   }
   public void run()
   {  Thread thisThread = Thread.currentThread();
      while (thread == thisThread)
      {  try
         {  System.out.println("Waiting for a client ..."); 
            addThread(server.accept()); }
         catch(IOException ioe)
         {  System.out.println("Server accept error: " + ioe); stop(); }
      }
   }
   public void start()
   {  if (thread == null)
      {  
         thread = new Thread(this); 
         thread.start();
      }
   }

   private boolean InitializeSDK()
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

   private void InitializeAccountList()
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
            int accountCount = (int) dataSnapshot.getChildrenCount();
            accounts = new Account[accountCount]; // create temp list

            int index = 0;
            for (DataSnapshot dataSnap : dataSnapshot.getChildren()) {
               String id = (String) dataSnap.child("id").getValue();
               String password = (String) dataSnap.child("password").getValue();
               accounts[index] = new Account(id, password); // Store to list
               index += 1;
            }

            System.out.println("Initialize account list success");

            if (!finishGetAccountList) // List got check
            {
               finishGetAccountList = true;
            }

            PrintAccountList();  // Debug list
         }
      
         @Override
         public void onCancelled(DatabaseError databaseError) {
            System.out.println("The read failed: " + databaseError.getCode());
         }
      });
   }

   private void PrintAccountList()
   {
      // If empty
      if (!finishGetAccountList)
      {
         System.out.println("Usage: account list not initialized");
         return;
      }

      System.out.println("------------------------------Account List-------------------------");
      System.out.println("Amount of account: " + accounts.length);
      System.out.println("------------------------------");
      System.out.println("--id--\t--password--");

      for (Account account : accounts)
      {
         System.out.println(account.GetId() + "\t" + account.GetPassword());
      }

      System.out.println("------------------------------end Account List-------------------------");
   }

   public void stop()
   {  if (thread != null)
      {  thread = null;
      }
   }
   private int findClient(int ID)
   {  for (int i = 0; i < clientCount; i++)
         if (clients[i].getID() == ID)
            return i;
      return -1;
   }
   public synchronized void handle(int ID, String input)
   {  if (input.equals(".bye"))
      {  clients[findClient(ID)].send(".bye");
         remove(ID); }
      else
         for (int i = 0; i < clientCount; i++)
            clients[i].send(ID + ": " + input);   
   }
   public synchronized void remove(int ID)
   {  int pos = findClient(ID);
      if (pos >= 0)
      {  ChatServerThread toTerminate = clients[pos];
         System.out.println("Removing client thread " + ID + " at " + pos);
         if (pos < clientCount-1)
            for (int i = pos+1; i < clientCount; i++)
               clients[i-1] = clients[i];
         clientCount--;
         try
         {  toTerminate.close(); }
         catch(IOException ioe)
         {  System.out.println("Error closing thread: " + ioe); }
         toTerminate.stopThread(); }
   }
   private void addThread(Socket socket)
   {  if (clientCount < clients.length)
      {  System.out.println("Client accepted: " + socket);
         clients[clientCount] = new ChatServerThread(this, socket);
         try
         {  clients[clientCount].open(); 
            clients[clientCount].start();  
            clientCount++; }
         catch(IOException ioe)
         {  System.out.println("Error opening thread: " + ioe); } }
      else
         System.out.println("Client refused: maximum " + clients.length + " reached.");
   }
   public static void main(String args[])
   {  ChatServer server = null;
      if (args.length != 1)
         System.out.println("Usage: java ChatServer port");
      else
         server = new ChatServer(Integer.parseInt(args[0]));
   }
}