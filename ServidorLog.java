import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServidorLog{

  private static int uniqueId;
  // an ArrayList to keep the list of the Client
  private ArrayList<ClientThread> al;
  // if I am in a GUI
  private ServerGUI sg;
  // to display time
  private SimpleDateFormat sdf;
  // the port number to listen for connection
  private int port;
  // the boolean that will be turned of to stop the server
  private boolean keepGoing;

  private static PublicKey clavePublica;
  private static PrivateKey clavePrivada;


    public ServidorLog(int port) {
        this(port, null);
    }
    public ServidorLog(int port, ServerGUI sg) {
        // GUI or not
        sg = null;
        this.sg = sg;
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
        //encriptamos las comunicaciones
        generarRSA();
    }
    public void start() {
        keepGoing = true;
        /* create socket server and wait for connection requests */
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if(!keepGoing)
                break;
                ClientThread t = new ClientThread(socket);  // make a thread of it
                String time = sdf.format(new Date());
                al.add(t);									// save it in the ArrayList
                t.writeObject(new ChatMessage(ChatMessage.KEY,clavePublica));

                  t.start();
                }
                // I was asked to stop
                try {
                    serverSocket.close();
                    for(int i = 0; i < al.size(); ++i) {
                        ClientThread tc = al.get(i);
                        try {
                            tc.sInput.close();
                            tc.sOutput.close();
                            tc.socket.close();
                        }
                        catch(IOException ioE) {
                            // not much I can do
                        }
                    }
                }
                catch(Exception e) {
                    display("Exception closing the server and clients: " + e);
                }
            }
            // something went bad
            catch (IOException e) {
                String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
                display(msg);
            }
        }
        private synchronized void broadcast(String message) {   // este metodo manda todo a todos los usuarios
            // add HH:mm:ss and \n to the message
            String time = sdf.format(new Date());
            String messageLf = time + " " + message + "\n";  //aqui esta el mensaje en sí
            // display message on console or GUI
            if (sg == null)
            System.out.print(messageLf); //lo imprime en consola
            else
            sg.appendRoom(messageLf);     // append in the room window

            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for (int i = al.size(); --i >= 0; ) {
                ClientThread ct = al.get(i);
                // try to write to the Client if it fails remove it from the list
                if (!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }

        private synchronized void broadcast(ChatMessage objeto) {   // este metodo manda todo a todos los usuarios
            // add HH:mm:ss and \n to the message
            if(objeto.getType()==1){
                String time = sdf.format(new Date());
                String messageLf = time + " " + objeto.getMessage() + "\n";
                objeto=new ChatMessage(ChatMessage.MESSAGE,messageLf);
                if (sg == null)
                System.out.print(messageLf); //lo imprime en consola
                else
                sg.appendRoom(messageLf);     // append in the room window
            }
            for (int i = al.size(); --i >= 0; ) {
                ClientThread ct = al.get(i);
                if (!ct.writeObject(objeto)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }


        }

        protected void stop() {
            keepGoing = false;
            // connect to myself as Client to exit statement
            // Socket socket = serverSocket.accept();
            try {
                new Socket("localhost", port);
            } catch (Exception e) {
                // nothing I can really do
            }
        }
        private void display(String msg) {
            String time = sdf.format(new Date()) + " " + msg;
            if (sg == null)
            System.out.println(time);
            else
            sg.appendEvent(time + "\n");
        }
        synchronized void remove(int id) {
            // scan the array list until we found the Id
            for (int i = 0; i < al.size(); ++i) {
                ClientThread ct = al.get(i);
                // found it
                if (ct.id == id) {
                    al.remove(i);
                    return;
                }
            }
        }
        public static void generarRSA() {
            try {
                //Establecemos las características de la clave (RSA 2048)
                KeyPairGenerator generadorDosClaves = KeyPairGenerator.getInstance("RSA"); //Tipo de algoritmo
                generadorDosClaves.initialize(2048); //Tamaño de la clave

                KeyPair pareja = generadorDosClaves.generateKeyPair();
                clavePrivada = pareja.getPrivate();
                clavePublica = pareja.getPublic();

            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
        public static void main(String[] args) {
            // start server on port 1500 unless a PortNumber is specified
            int portNumber = 1501;
            switch (args.length) {
                case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java ServerLog [portNumber]");
                    return;
                }
                case 0:
                break;
                default:
                System.out.println("Usage is: > java ServerLog [portNumber]");
                return;

            }
            // create a server object and start it
            ServidorLog server = new ServidorLog(portNumber);
            server.start();
        }

                /**
                * One instance of this thread will run for each client
                */
                class ClientThread extends Thread {
                    // the socket where to listen/talk
                    Socket socket;
                    ObjectInputStream sInput;
                    ObjectOutputStream sOutput;
                    // my unique id (easier for deconnection)
                    int id;
                    // the Username of the Client
                    String username;
                    // the only type of message a will receive
                    ChatMessage cm;
                    // the date I connect
                    String date;
                    String auxsalt=null;
                    boolean keyflag=false;

                    // Constructore
                    ClientThread(Socket socket) {
                        // a unique id
                        id = ++uniqueId;
                        this.socket = socket;
                        /* Creating both Data Stream */
                        System.out.println("Thread trying to create Object Input/Output Streams");
                        try {
                            // create output first
                            sOutput = new ObjectOutputStream(socket.getOutputStream());
                            sInput = new ObjectInputStream(socket.getInputStream());
                            // read the username
                            username = (String) sInput.readObject();
                            display(username + " just connected.");
                        } catch (IOException e) {
                            display("Exception creating new Input/output Streams: " + e);
                            return;
                        }
                        // have to catch ClassNotFoundException
                        // but I read a String, I am sure it will work
                        catch (ClassNotFoundException e) {
                        }
                        date = new Date().toString() + "\n";
                    }
                    //hará una sola lectura del cliente para nosotros
                    private void runkey(){

                        try {
                            cm = (ChatMessage) sInput.readObject();
                        } catch (IOException e) {
                            display(username + " Exception reading Streams: " + e);
                        } catch (ClassNotFoundException e2) {
                        }
                        // the messaage part of the ChatMessage
                        String message = cm.getMessage();
                        if(message.equalsIgnoreCase("~0~")){
                        }
                    }

                    // what will run forever
                    public void run() {
                        // to loop until LOGOUT
                        boolean keepGoing = true;
                        while (keepGoing) {
                            // read a String (which is an object)
                            try {
                                cm = (ChatMessage) sInput.readObject();
                            } catch (IOException e) {
                                display(username + " Exception reading Streams: " + e);
                                break;
                            } catch (ClassNotFoundException e2) {
                                break;
                            }
                            // the messaage part of the ChatMessage
                            //String message = cm.getMessage();
                            ClientThread ct=null;
                            for (int i = 0; i < al.size(); ++i) {
                                ClientThread ctaux = al.get(i);
                                // found it
                                if (ctaux.id == id) {
                                     ct=ctaux;
                                     break;
                                }
                            }
                            String nickname;
                            String pass;
                            Connection connection;
                            ResultSet resultSet;

                            // Switch on the type of message receive
                            switch (cm.getType()) {
                              case ChatMessage.LOGIN:
                                  nickname=cm.getNickname();
                                  pass=cm.getPass();
                                  nickname=desencriptarconPrivada(nickname);
                                  pass=desencriptarconPrivada(pass);

                                   
                                  resultSet = null;

                                  try {
                                      Class.forName("com.mysql.jdbc.Driver");
                                      String url = "jdbc:mysql://localhost:3306/lorikeet";
                                      connection = DriverManager.getConnection(url, "Admin", "bL5exzEtMfcLWMHU");

                                      Statement statement = connection.createStatement();

                                      String selectSql = "SELECT * FROM usuario WHERE '"+nickname+"'=nickname and '"+pass+auxsalt+"'=Clave";
                                      resultSet = statement.executeQuery(selectSql);

                                       if(resultSet.next()==false){
                                         ct.writeObject(new ChatMessage(ChatMessage.MESSAGE, "??"));
                                       }
                                       else{
                                         ct.writeObject(new ChatMessage(ChatMessage.MESSAGE, "//"));
                                       }

                                  } catch (SQLException ex) {
                                      ex.printStackTrace();
                                  } catch (ClassNotFoundException ex) {
                                      ex.printStackTrace();
                                  }


                              break;
                              case ChatMessage.REGISTRO:

                                  nickname=cm.getNickname();
                                  pass=cm.getPass();
                                  nickname=desencriptarconPrivada(nickname);
                                  pass=desencriptarconPrivada(pass);


                                   resultSet = null;

                                  try {
                                      Class.forName("com.mysql.jdbc.Driver");
                                      String url = "jdbc:mysql://localhost:3306/lorikeet";
                                      connection = DriverManager.getConnection(url, "Admin", "bL5exzEtMfcLWMHU");

                                      String insertSql = "INSERT INTO usuario (Nickname, Clave) VALUES ('"+nickname+"','"+pass+"')";
                                      PreparedStatement prepsInsertProduct = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                                      prepsInsertProduct.execute();

                                  } catch (SQLException ex) {
                                      ex.printStackTrace();
                                  } catch (ClassNotFoundException ex) {
                                      ex.printStackTrace();
                                  }
                                  ct.writeObject(new ChatMessage(ChatMessage.MESSAGE, "~~"));

                              break;
                                case ChatMessage.MESSAGE:
                                  nickname=cm.getMessage();
                                  nickname=desencriptarconPrivada(nickname);

                                  resultSet = null;

                                  try {
                                      Class.forName("com.mysql.jdbc.Driver");
                                      String url = "jdbc:mysql://localhost:3306/lorikeet";
                                      connection = DriverManager.getConnection(url, "Admin", "bL5exzEtMfcLWMHU");

                                      Statement statement = connection.createStatement();

                                      String selectSql = "SELECT Clave FROM usuario WHERE '"+nickname+"'=nickname";
                                      resultSet = statement.executeQuery(selectSql);

                                       if(resultSet.next()==false){
                                         ct.writeObject(new ChatMessage(ChatMessage.MESSAGE, "??"));
                                       }
                                       else{
                                            String cortar=resultSet.getString(1);
                                            cortar=cortar.substring(64);
                                            auxsalt=cortar;
                                         ct.writeObject(new ChatMessage(ChatMessage.MESSAGE, "??ss??"+cortar));
                                       }

                                  } catch (SQLException ex) {
                                      ex.printStackTrace();
                                  } catch (ClassNotFoundException ex) {
                                      ex.printStackTrace();
                                  }
                                break;
                                case ChatMessage.CIPHERMESSAGE:

                                break;
                                case ChatMessage.LOGOUT:
                                display(username + " disconnected with a LOGOUT message.");
                                keepGoing = false;
                                break;


                            }
                        }
                        // remove myself from the arrayList containing the list of the
                        // connected Clients
                        remove(id);
                        close();
                    }

                    // try to close everything
                    private void close() {
                        // try to close the connection
                        try {
                            if (sOutput != null) sOutput.close();
                        } catch (Exception e) {
                        }
                        try {
                            if (sInput != null) sInput.close();
                        } catch (Exception e) {
                        }
                        ;
                        try {
                            if (socket != null) socket.close();
                        } catch (Exception e) {
                        }
                    }
                    public String desencriptarconPrivada(String cosa) {
                        String prueba=null;
                        try {
                             //La clave en bytes

                            Cipher cifrado = Cipher.getInstance("RSA");
                            cifrado.init(Cipher.DECRYPT_MODE, clavePrivada); //Le decimos explícitamente que queremos encriptar

                            byte[] CifradoBytes = Base64.getDecoder().decode(cosa);
                              prueba = new String(cifrado.doFinal(CifradoBytes));

                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                        return prueba;
                    }
                    /*
                    * Write a String to the Client output stream
                    */
                    private boolean writeMsg(String msg) {
                        // if Client is still connected send the message to it
                        if(!socket.isConnected()) {
                            close();
                            return false;
                        }
                        // write the message to the stream
                        try {
                            sOutput.writeObject(msg);
                        }
                        // if an error occurs, do not abort just inform the user
                        catch(IOException e) {
                            display("Error sending message to " + username);
                            display(e.toString());
                        }
                        return true;
                    }

                    /*
                    * Write a Object to the Client output stream
                    */
                    private boolean writeObject(ChatMessage cm) {
                        // if Client is still connected send the message to it
                        if(!socket.isConnected()) {
                            close();
                            return false;
                        }
                        // write the message to the stream
                        try {
                            sOutput.writeObject(cm);
                        }
                        // if an error occurs, do not abort just inform the user
                        catch(IOException e) {
                            display("Error sending file to " + username);
                            display(e.toString());
                        }
                        return true;
                    }
                }
            }
