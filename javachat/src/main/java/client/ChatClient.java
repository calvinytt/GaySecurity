package client;

import java.net.*;
import java.io.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.swing.JOptionPane;
import javax.crypto.SecretKeyFactory;
import java.util.Base64;

public class ChatClient implements Runnable
{  private Socket socket              = null;
   private volatile Thread thread     = null;
   private BufferedReader   console   = null;
   private DataOutputStream streamOut = null;
   private ChatClientThread client    = null;

   BufferedReader input;
   PrintWriter output;

   private String plainText = null;
   KeyGenerator keyGenerator;
   private static Cipher cipherDES = null;
   SecretKey secretKey;

   //public and private
   private KeyPair pair;
   KeyPairGenerator generateKeyPair;
   private PrivateKey privateKey;
   private PublicKey publicKey;
   private byte[] bypublicKey;
   private byte[] byprivateKey;
   private Cipher cipherRSA;

   public ChatClient(String serverName, int serverPort) throws NoSuchAlgorithmException, NoSuchPaddingException
   {  System.out.println("Establishing connection. Please wait ...");
      try
      {  
         socket = new Socket(serverName, serverPort);         
         System.out.println("Connected: " + socket);

         genRSAKey();

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
//            System.out.println(decryptedText);
         {

            cipherDES = Cipher.getInstance("DESede");
            plainText = console.readLine();
//      System.out.println(console.readLine());

            byte[] plainTextByte = plainText.getBytes("UTF8");
            byte[] encryptedBytes = encrypt(plainTextByte, secretKey);
            String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
            streamOut.writeUTF(encryptedText);
            streamOut.flush();
         }
         catch(IOException ioe)
         {  System.out.println("Sending error: " + ioe.getMessage());
            stop();
         }catch (Exception e){
            System.out.println("Sending error: " + e.getMessage());
            stop();
         }
      }
   }
   public void handle(String msg)
   {  try {
         String[] splitInput = msg.split(" ");
         if (msg.equals(".bye")) {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
         } else if (msg.equals("public key")) {
            byte[] bypublicKey = this.publicKey.getEncoded();
            byte[] byprivateKey = this.privateKey.getEncoded();
            String publicKeyText = Base64.getEncoder().encodeToString(bypublicKey);
            streamOut.writeUTF("public : " + publicKeyText);
            streamOut.flush();
         } else if (msg.equals("secret key")) {
            genSecretKey();
            streamOut.writeUTF("secret key gen success");
            streamOut.flush();
         } else if (splitInput[0].equals("public")) {
            byte[] newbypublicKey = Base64.getDecoder().decode(splitInput[2]);
            PublicKey newPublicKey =
                  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(newbypublicKey));
            String secretKeyText = encryptTextByPublic(Base64.getEncoder().encodeToString(secretKey.getEncoded()), newPublicKey);
            streamOut.writeUTF("secret : " + secretKeyText);
            streamOut.flush();
         } else if (splitInput[0].equals("secret")) {
            byte[] bySecretKey = Base64.getDecoder().decode(decrypTextByPrivate(splitInput[2], privateKey));
            secretKey = new SecretKeySpec(bySecretKey, 0, bySecretKey.length, "DESede");
         } else {
            byte[] decryptedBytes = decrypt(Base64.getDecoder().decode(splitInput[1]), secretKey);
            String decryptedText = new String(decryptedBytes, "UTF8");
            System.out.println(splitInput[0] + decryptedText);
         }
      } catch (Exception e){
         System.out.println("Sending error: " + e.getMessage());
      }
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

   public void genRSAKey() throws NoSuchAlgorithmException,NoSuchPaddingException{
      KeyPairGenerator generateKeyPair = KeyPairGenerator.getInstance("RSA");
      generateKeyPair.initialize(1024);
      pair = generateKeyPair.generateKeyPair();
      privateKey = pair.getPrivate();
      publicKey = pair.getPublic();
      this.cipherRSA = Cipher.getInstance("RSA");
   }
   public void genSecretKey(){
      try {
         keyGenerator = KeyGenerator.getInstance("DESede");
         keyGenerator.init(168);
         secretKey = keyGenerator.generateKey();

      }catch (NoSuchAlgorithmException e){
         System.out.println("Sending error: " + e.getMessage());
      }
   }
   public byte[] encrypt(byte[] plainTextByte, SecretKey secretKey)
           throws Exception {
      cipherDES.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encryptedBytes = cipherDES.doFinal(plainTextByte);
      return encryptedBytes;
   }
   
   public byte[] decrypt(byte[] encryptedBytes, SecretKey secretKey)
           throws Exception {
      cipherDES.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decryptedBytes = cipherDES.doFinal(encryptedBytes);
      return decryptedBytes;
   }

   public String encryptTextByPrivate(String msg, PrivateKey key)
           throws NoSuchAlgorithmException, NoSuchPaddingException,
           UnsupportedEncodingException, IllegalBlockSizeException,
           BadPaddingException, InvalidKeyException {
      this.cipherRSA.init(Cipher.ENCRYPT_MODE, key);
      return Base64.getEncoder().encodeToString(cipherRSA.doFinal(msg.getBytes("UTF-8")));
   }
   public String encryptTextByPublic(String msg, PublicKey key)
           throws NoSuchAlgorithmException, NoSuchPaddingException,
           UnsupportedEncodingException, IllegalBlockSizeException,
           BadPaddingException, InvalidKeyException {
      this.cipherRSA.init(Cipher.ENCRYPT_MODE, key);
      return Base64.getEncoder().encodeToString(cipherRSA.doFinal(msg.getBytes("UTF-8")));
   }

   public String decryptText(String msg, PublicKey key)
           throws InvalidKeyException, UnsupportedEncodingException,
           IllegalBlockSizeException, BadPaddingException {
      this.cipherRSA.init(Cipher.DECRYPT_MODE, key);
      return Base64.getEncoder().encodeToString(cipherRSA.doFinal(msg.getBytes("UTF-8")));
   }
   public String decrypTextByPrivate(String msg, PrivateKey key)
           throws InvalidKeyException, UnsupportedEncodingException,
           IllegalBlockSizeException, BadPaddingException {
      this.cipherRSA.init(Cipher.DECRYPT_MODE, key);
      return new String(cipherRSA.doFinal(Base64.getDecoder().decode(msg)), "UTF-8");
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
         throws NumberFormatException, NoSuchAlgorithmException, NoSuchPaddingException
   {  ChatClient client = null;
      if (args.length != 2)
         System.out.println("Usage: java ChatClient host port");
      else
         client = new ChatClient(args[0], Integer.parseInt(args[1]));
   }
}
