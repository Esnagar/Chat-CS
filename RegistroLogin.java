import java.sql.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.nio.file.*;

public class RegistroLogin {
    /*public static void main(String[] args) {
        Connection connection;
        ResultSet resultSet = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/lorikeet";
            connection = DriverManager.getConnection(url, "Admin", "bL5exzEtMfcLWMHU");

            String insertSql = "INSERT INTO usuario (Nickname, Clave) VALUES ('Esther','London')";
            PreparedStatement prepsInsertProduct = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            prepsInsertProduct.execute();

            Statement statement = connection.createStatement();

            String selectSql = "SELECT * FROM usuario";
            resultSet = statement.executeQuery(selectSql);

            while (resultSet.next()) {
                System.out.println(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }*/


    private ObjectInputStream sInput;        // to read from the socket
    private ObjectOutputStream sOutput;        // to write on the socket
    private Socket socket;
    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;

    private static PublicKey clavePublica;
    private static String[] trozos;

    RegistroLogin(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    RegistroLogin(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        cg = null;
        this.cg = cg;
    }

    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }
        // if it failed not much I can so
        catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        /* Creating both Data Stream */
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    private void display(String msg) {
        if (cg == null)
        System.out.println(msg);      // println in console mode
        else
        cg.append(msg + "\n");        // append to the ClientGUI JTextArea (or whatever)
    }

    /*
    * To send a message to the server  //importante
    */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if (sInput != null) sInput.close();
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (sOutput != null) sOutput.close();
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
        } // not much else I can do

        // inform the GUI
        if (cg != null)
        cg.connectionFailed();

    }

    public static void main(String[] args) {
        // default values
        int portNumber = 1501;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
            serverAddress = args[2];
            // > javac Client username portNumber
            case 2:
            try {
                portNumber = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Invalid port number.");
                System.out.println("Usage is: > java RegistroLogin [username] [portNumber] [serverAddress]");
                return;
            }
            // > javac Client username
            case 1:
            userName = args[0];
            // > java Client
            case 0:
            break;
            // invalid number of arguments
            default:
            System.out.println("Usage is: > java RegistroLogin [username] [portNumber] {serverAddress]");
            return;
        }
        // create the Client object
        RegistroLogin client = new RegistroLogin(serverAddress, portNumber, userName);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if (!client.start())
        return;

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // break to do the disconnect
                break;
            }
            if(msg.contains("LOGIN")){
              trozos = msg.split(" ");

                trozos[1]=encriptarconPublica(trozos[1]);
               client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, trozos[1]));

            }
            if(msg.contains("REGISTRO")){
              trozos = msg.split(" ");
              byte[] salt=null;
              if(trozos[2].equalsIgnoreCase(trozos[3])){
                try{
                    salt = getSalt();
                    System.out.println(Base64.getEncoder().encodeToString(salt));
                }
                catch (NoSuchAlgorithmException e){
                  e.printStackTrace();
                  }
                trozos[2] = get_SHA_256_SecurePassword(trozos[2], salt);
                trozos[1]=encriptarconPublica(trozos[1]);
                trozos[2]=encriptarconPublica(trozos[2]+Base64.getEncoder().encodeToString(salt));
              client.sendMessage(new ChatMessage(ChatMessage.REGISTRO, trozos[1],trozos[2]));
                      }
                    }
            }
            // done disconnect
            client.disconnect();
        }


        class ListenFromServer extends Thread {

            public void run() {

                while (true) {
                    try {

                        ChatMessage aux = (ChatMessage) sInput.readObject();
                            //String msg=aux.getMessage();
                            //System.out.println(msg);
                            if(aux.getType()==4){
                                clavePublica=aux.getKey();
                            }
                            if(aux.getType()==1){
                                if(aux.getMessage().equalsIgnoreCase("//")){
                                  System.out.println("Se te ha validado como un usuario correcto");
                                  int portNumber = 1500;
                                  String serverAddress = "localhost";
                                  String userName = "Anonymous";
                                  Client client = new Client(serverAddress, portNumber, userName);
                                }
                                if(aux.getMessage().contains("??ss??")){
                                    byte[] salt=null;
                                    String saltt=aux.getMessage().substring(6);
                                    salt=Base64.getDecoder().decode(saltt);
                                    System.out.println(saltt);
                                  trozos[2] = get_SHA_256_SecurePassword(trozos[2], salt);
                                    trozos[2]=encriptarconPublica(trozos[2]);
                                  sOutput.writeObject(new ChatMessage(ChatMessage.LOGIN, trozos[1],trozos[2]));
                                }
                                if(aux.getMessage().equalsIgnoreCase("??")){
                                    System.out.println("El usuario indicado no existe en el sistema");
                                }
                                if(aux.getMessage().equalsIgnoreCase("~~")){
                                  System.out.println("Has sido registrado en el sistema");
                                }
                            }

                        }

                     catch (IOException e) {
                        display("Server has close the connection: " + e);
                        if (cg != null)
                        cg.connectionFailed();
                        break;
                    }
                    // can't happen with a String object but need the catch anyhow
                    catch (ClassNotFoundException e2) {
                    }
                }
            }
        }
        public static String encriptarconPublica(String cosa) {
            String aesCifrado = null;
            try {
                Cipher cifrado = Cipher.getInstance("RSA");
                cifrado.init(Cipher.ENCRYPT_MODE, clavePublica); //Le decimos explícitamente que queremos encriptar

                aesCifrado = Base64.getEncoder().encodeToString(cifrado.doFinal(cosa.getBytes("UTF-8")));


            } catch (Exception ex) {
                System.out.println(ex);
            }

            return aesCifrado;
        }
        private static String get_SHA_256_SecurePassword(String passwordToHash, byte[] salt){
     String generatedPassword = null;
     try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         md.update(salt);
         byte[] bytes = md.digest(passwordToHash.getBytes());
         StringBuilder sb = new StringBuilder();
         for(int i=0; i< bytes.length ;i++)
         {
             sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
         }
         generatedPassword = sb.toString();
     }
     catch (NoSuchAlgorithmException e)
     {
         e.printStackTrace();
     }
     return generatedPassword;
 }

 private static byte[] getSalt() throws NoSuchAlgorithmException{
      SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
      byte[] salt = new byte[16];
      sr.nextBytes(salt);
      return salt;
  }

}
