package server;

import java.net.*;
import java.io.*;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;

public class ChatServer implements Runnable
{  private ChatServerThread clients[] = new ChatServerThread[50];
   // private ServerSocket server = null;
   private SSLServerSocket server = null;
   private volatile Thread  thread = null;
   private int clientCount = 0;

   private DatabaseConnect dbConnect = new DatabaseConnect();
   private AccountList accounts = new AccountList();

   private SendMailSSL mailSSL = new SendMailSSL();  // send otp
   private Random rand = new Random();
   private int otpLength = 32;
   private float numberPercent = 0.16129032f;  // 10 / (10 + 26 + 26)

   BufferedReader input;
   PrintWriter output;

   private static final String SERVER_KEY_STORE_PASSWORD  = "password";
   private static final String SERVER_TRUST_KEY_STORE_PASSWORD = "password";

   private final String path = "src/main/java/server/";

   // 32 letter of number or alpha
   public String RandomOTP()
   {
      String otp = "";

      for(int index = 0; index < otpLength; index++)
      {
         float randFloat = rand.nextFloat();

         if (randFloat < numberPercent)  // rand number
         {
            otp += (char) (rand.nextInt(10) + 48);   // 0-9 + 48
         }
         else  // rand alpha
         {
            otp += (char) (rand.nextInt(26) + 65 + (rand.nextInt(2) == 0 ? 0 : 32)) ;   // 0-25 + 65 + 0/32(upper/lower)
         }
      }

      return otp;
   }

   public boolean Login(String encryptedId, String encryptedPassword)
   {
      // Encrypt id and pw
      String id = encryptedId;
      String password = encryptedPassword;

      // Check account correct
      return accounts.Contain(id, password);
   }

   public ChatServer(int port)
   {  // Firebase admin
      if (!dbConnect.InitializeSDK()) {
         System.out.println("Usage: initialize SDK fail");
         return;
      }

      // Get all account data from db
      dbConnect.InitializeAccountList(accounts);

      // Start socket until getting account data finish
      try {
         while (accounts.IsEmpty()) {
            Thread.sleep(1000);
         }
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
      } finally {
         System.out.println("Binding to port " + port + ", please wait  ...");
         // server = new ServerSocket(port);
         init(port); // socket + cert
         System.out.println("Server started: " + server);
         start();
      }
   }
   public void run()
   {  Thread thisThread = Thread.currentThread();
      while (thread == thisThread)
      {  try
         {  
            System.out.println("Waiting for a client ..."); 
            addThread(server.accept());
         }
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

   public void stop()
   {  if (thread != null)
      {  thread = null;
      }

      // validate login socket
      try {
         output.close();
         input.close();
      }
      catch(IOException ioe)
      {  System.out.println("Error closing ..."); }
      
   }
   private int findClient(int ID)
   {  for (int i = 0; i < clientCount; i++)
         if (clients[i].getID() == ID)
            return i;
      return -1;
   }
   public synchronized void handle(int ID, String input)
   {  
      String[] splitInput = input.split(" ");
   //      System.out.println(clientCount);
      System.out.println(input);
      if (input.equals(".bye")) {
         clients[findClient(ID)].send(".bye");
         remove(ID);
      } else if (input.equals("Client Connect")) {
         if (clientCount > 1) {
            clients[clientCount - 1].send("public key");
         }else{
            clients[0].send("secret key");
         }
      }else if (splitInput[0].equals("public")) {
         System.out.println(splitInput[2]);
         clients[0].send(input);
      }  else if (input.equals("secret key gen success")) {
         System.out.println(input);
      } else if (splitInput[0].equals("secret")) {
         System.out.println(splitInput[2]);
         clients[clientCount - 1].send(input);;
      } else {
         for (int i = 0; i < clientCount; i++)
            clients[i].send(ID + ": " + input);
      }
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
      {  
         boolean validAccount = false;
         boolean validOtp = false;

         // Validate account
         try {
            // System.out.println("Receive account id and password");
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String id = input.readLine();
            System.out.println("id: " + id);
            String password = input.readLine();
            System.out.println("password: " + password);

            // System.out.println("Validate account");
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            validAccount = accounts.Contain(id, password);

            if (validAccount)
            {
               // random otp
               String otp = RandomOTP();

               // Set to db
               dbConnect.SaveOTP(id, otp);

               // Send to mail
               mailSSL.SendMail(accounts.GetMail(id), otp);
               
               // Receive otp
               output.println("Welcome, " + id);
               output.flush();   // progress flush

               String inputOtp = input.readLine();
               System.out.println("otp: " + inputOtp);

               if (otp.equals(inputOtp))
               {
                  output.println("Correct otp");
                  validOtp = true;
               }
               else
               {
                  output.println("Login Failed");
               }
            }
            else
            {
               output.println("Login Failed");
            }

            output.flush();
         } catch (Exception e) {
            e.printStackTrace();
         }

         if (validAccount && validOtp)
         {
            // Accept client
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try
            {  clients[clientCount].open(); 
               clients[clientCount].start();  
               clientCount++;
            }
            catch(IOException ioe)
            {  System.out.println("Error opening thread: " + ioe); } }
         }
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

   public void init(int port) {
      try {
         SSLContext ctx = SSLContext.getInstance("SSL");
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
         TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
         KeyStore ks = KeyStore.getInstance("JKS");
         KeyStore tks = KeyStore.getInstance("JKS");
         ks.load(new FileInputStream(path + "kserver.keystore"), SERVER_KEY_STORE_PASSWORD.toCharArray());
         tks.load(new FileInputStream(path + "tserver.keystore"), SERVER_TRUST_KEY_STORE_PASSWORD.toCharArray());
         kmf.init(ks, SERVER_KEY_STORE_PASSWORD.toCharArray());
         tmf.init(tks);
         ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
         server = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(port);
         server.setNeedClientAuth(true);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}