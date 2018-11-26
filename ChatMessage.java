import java.io.*;
import java.security.*;
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
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, FILE = 3, KEY = 4, CIPHERMESSAGE = 5;
	private int type;
	private String message;
	private byte[] contenido;
	private PublicKey clave;

	// constructor
	ChatMessage(int type, PublicKey clave){
		if(type==4){
			this.type = type;
			this.message = "~0~";
			this.clave=clave;
		}
	}
	ChatMessage(int type, String message) {
		if(type==3){ //FILE: nombreejemplo
			this.message=message;
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
	public void setContenido(byte[] setear){
		this.contenido=setear;
	}
	String getMessage() {
		return message;
	}
	byte[] getContenido(){
		return contenido;
	}
	PublicKey getKey(){
		return clave;
	}
}
