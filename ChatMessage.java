import java.io.*;
import java.nio.file.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server.
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no
 * need to count bytes or to wait for a line feed at the end of the frame
 */
public class ChatMessage implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	// WHOISIN to receive the list of the users connected
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server
	//FILE contains a FILE
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, FILE = 3;
	private int type;
	private String message;
	private byte[] contenido;

	// constructor
	ChatMessage(int type, String message) {
		if(type==3){//FILE: KSDLAKMDLAMDAML
			String archivo=message.substring(6,message.length());
			this.message=archivo.substring(archivo.lastIndexOf("\\"));
			System.out.println(archivo);
			File f = new File(archivo);
			byte[] content=null;
			try{
			content = Files.readAllBytes(f.toPath());
		}
		catch (IOException ex) {
            System.out.println("Problema con el archivo");
        }
			this.contenido=content;
			this.type = type;
		}
		else{
		this.type = type;
		this.message = message;
	}
	}

	// getters
	int getType() {
		return type;
	}
	String getMessage() {
		return message;
	}
	byte[] getContenido(){
		return contenido;
	}
}
