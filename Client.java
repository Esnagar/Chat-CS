import java.net.*;
import java.io.*;
import java.util.*;
import javax.crypto.*;
import java.security.*;

/* The Client that can be run both as a console or a GUI */
public class Client {

    // for I/O
    private ObjectInputStream sInput;        // to read from the socket
    private ObjectOutputStream sOutput;        // to write on the socket
    private Socket socket;
    private static SecretKey claveAES; //Clave para encriptar los mensajes
    private static PublicKey clavePublica;
    private static PrivateKey clavePrivada;
    private String claveAESEncriptada;

    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;

    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  username: the username
     */
    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    /*
     * Constructor call when used from a GUI
     * in console mode the ClienGUI parameter is null
     */
    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        cg = null;
        this.cg = cg;
    }

    /*
     * To start the dialog
     */
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

    /*
     * To send a message to the console or the GUI
     */
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

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
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

    /*
     * To start the Client in console mode use one of the following command
     * > java Client
     * > java Client username
     * > java Client username portNumber
     * > java Client username portNumber serverAddress
     * at the console prompt
     * If the portNumber is not specified 1500 is used
     * If the serverAddress is not specified "localHost" is used
     * If the username is not specified "Anonymous" is used
     * > java Client
     * is equivalent to
     * > java Client Anonymous 1500 localhost
     * are eqquivalent
     *
     * In console mode, if an error occurs the program simply stops
     * when a GUI id used, the GUI is informed of the disconnection
     */
    public static void main(String[] args) {
        // default values
        int portNumber = 1500;
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
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
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
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
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
            // message WhoIsIn
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            } else {                // default to ordinary message
                msg = encriptarMensaje(msg);
                desencriptarMensaje(msg);
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // done disconnect
        client.disconnect();
    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    // if console mode print the message and add back the prompt
                    if (cg == null) {
                        generarRSA();
                        System.out.println(msg);
                        msg = msg.substring(9, msg.length() - 1);
                        if (msg.equalsIgnoreCase("Eres el primero que chupi")) {
                            System.out.println("Soy el primero viva");
                            generarAES();
                            claveAESEncriptada = encriptarAES(claveAES);
                        }
                        if(!msg.equalsIgnoreCase("Eres el primero que chupi")){
							              System.out.print("> ");
						            }
                    } else {
                        cg.append(msg);
                    }
                } catch (IOException e) {
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


    public static void generarAES() {
        try {
            //Establecemos las características de la clave (AES 128)
            KeyGenerator generadorClave = KeyGenerator.getInstance("AES"); //Tipo de algoritmo
            generadorClave.init(128); //Tamaño de la clave

            claveAES = generadorClave.generateKey(); //Genera la clave AES
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }


    public static String encriptarAES(SecretKey aes) {
        byte[] aesCifrado = null;
        try {
            byte[] aesBytes = aes.getEncoded(); //La clave en bytes

            //Pasamos a cifrar la clave generada con AES
            Cipher cifrado = Cipher.getInstance("RSA");
            cifrado.init(Cipher.PUBLIC_KEY, clavePublica); //Le decimos explícitamente que queremos encriptar

            aesCifrado = cifrado.doFinal(aesBytes); //Convertimos el mensaje en bytes

            //Mostramos por pantalla los resultados
            System.out.println("Clave original: " + aes);
            System.out.println("Clave en bytes: " + aesBytes);
            System.out.println("Clave encriptada: ");
            for (int i = 0; i < aesCifrado.length; i++) {
                System.out.print(aesCifrado[i] + " ");
            }

            System.out.println();

        } catch (Exception ex) {
            System.out.println(ex);
        }

        return new String(aesCifrado);
    }


    public static String encriptarMensaje(String textoPlano) {

        String textoCifrado = null;

        try {
            //Ahora que tenemos la clave, pasamos a cifrar el mensaje
            Cipher cifrado = Cipher.getInstance("AES");
            cifrado.init(Cipher.ENCRYPT_MODE, claveAES); //Le decimos explícitamente que queremos encriptar

            textoCifrado = Base64.getEncoder().encodeToString(cifrado.doFinal(textoPlano.getBytes("UTF-8")));

            //Mostramos por pantalla los resultados
            System.out.println("Mensaje original: " + textoPlano);
            System.out.println("Mensaje encriptado: " + textoCifrado);
            /*for (int i = 0; i < textoCifrado.length; i++) {
                System.out.print(textoCifrado[i] + " ");
            }*/
            System.out.println();

        } catch (Exception ex) {
            System.out.println(ex);
        }

        return new String(textoCifrado);
    }


    public static String desencriptarMensaje(String textoCifrado) {
        String textoPlano = null;

        try {
            //Ahora que tenemos la clave, pasamos a descifrar el mensaje
            Cipher cifrado = Cipher.getInstance("AES");
            cifrado.init(Cipher.DECRYPT_MODE, claveAES); //Le decimos explícitamente que queremos desencriptar

            byte[] base64desencriptar = Base64.getDecoder().decode(textoCifrado);
            textoPlano = new String(cifrado.doFinal(base64desencriptar));

            //Mostramos por pantalla los resultados
            System.out.println("Mensaje cifrado: " + textoCifrado);
            System.out.println("Mensaje en claro: " + textoPlano);
            /*for (int i = 0; i < textoPlano.length; i++) {
                System.out.print(textoPlano[i] + " ");
            }*/
            System.out.println();

        } catch (Exception ex) {
            System.out.println(ex);
        }

        return textoPlano;
    }
}
