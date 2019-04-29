package client;

import java.net.*;
import java.io.*;

import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ChatClient implements Runnable
{  private Socket socket              = null;
   private volatile Thread thread     = null;
   private BufferedReader   console   = null;
   private DataOutputStream streamOut = null;
   private ChatClientThread client    = null;

   BufferedReader input;
   PrintWriter output;

   private static final String CLIENT_KEY_STORE_PASSWORD  = "password";
   private static final String CLIENT_TRUST_KEY_STORE_PASSWORD = "password";

   private final String path = "client/";

   public ChatClient(String serverName, int serverPort)
   {  System.out.println("Establishing connection. Please wait ...");
      try
      {  
         // socket = new Socket(serverName, serverPort);
         init(serverName,serverPort);  // socket + cert
         
         
         System.out.println("Connected: " + socket);
         start();
      }
      catch(UnknownHostException uhe)
      {  System.out.println("Host unknown: " + uhe.getMessage()); }
      catch(IOException ioe)
      {  System.out.println("Unexpected exception: " + ioe.getMessage()); }
   }
   public void run()
   {  Thread thisThread = Thread.currentThread();
      while (thread == thisThread)
      while (thread != null)
      {  try
         {  streamOut.writeUTF(console.readLine());
            streamOut.flush();
         }
         catch(IOException ioe)
         {  System.out.println("Sending error: " + ioe.getMessage());
            stop();
         }
      }
   }
   public void handle(String msg)
   {  if (msg.equals(".bye"))
      {  System.out.println("Good bye. Press RETURN to exit ...");
         stop();
      }
      else
         System.out.println(msg);
   }
   public void start() throws IOException
   {  console   = new BufferedReader(new InputStreamReader(System.in));
      streamOut = new DataOutputStream(socket.getOutputStream());
      if (thread == null)
      {  
         // Sending account id and password
         output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

         System.out.print("id: ");
         String id = console.readLine();
         output.println(id);

         System.out.print("password: ");
         String password = console.readLine();
         output.println(password);

         output.flush();

         // Receiveing login result
         input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

         String response = input.readLine();
         System.out.println("This is the response: " + response);

         // Send otp
         if (!response.equals("Login Failed"))
         {
            System.out.print("otp: ");
            String otp = console.readLine();
            output.println(otp);
   
            output.flush();
   
            // Receive response
            String otpResponse = input.readLine();
            System.out.println("This is the response: " + otpResponse);
   
            // Open thread if success
            client = new ChatClientThread(this, socket);
            thread = new Thread(this);                   
            thread.start();
         }
      }
   }
   public void stop()
   {  if (thread != null)
      {  thread = null;
      }
      try
      {  if (console   != null)  console.close();
         if (streamOut != null)  streamOut.close();
         if (socket    != null)  socket.close();

         // validate login socket
         output.close();
         input.close();
      }
      catch(IOException ioe)
      {  System.out.println("Error closing ..."); }
      client.close();  
      client.stopThread();
   }
   public static void main(String args[])
   {  ChatClient client = null;
      if (args.length != 2)
         System.out.println("Usage: java ChatClient host port");
      else
         client = new ChatClient(args[0], Integer.parseInt(args[1]));
   }

   public void init(String serverName, int serverPort) {
      try {
         SSLContext ctx = SSLContext.getInstance("SSL");
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
         TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
         KeyStore ks = KeyStore.getInstance("JKS");
         KeyStore tks = KeyStore.getInstance("JKS");
         ks.load(new FileInputStream(path + "kclient.keystore"), CLIENT_KEY_STORE_PASSWORD.toCharArray());
         tks.load(new FileInputStream(path + "tclient.keystore"), CLIENT_TRUST_KEY_STORE_PASSWORD.toCharArray());
         kmf.init(ks, CLIENT_KEY_STORE_PASSWORD.toCharArray());
         tmf.init(tks);
         ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
         socket = (SSLSocket) ctx.getSocketFactory().createSocket(serverName, serverPort);
      } catch (Exception e) {
         System.out.println(e);
      }
   }
}