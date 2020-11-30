package es.ubu.lsi.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import es.ubu.lsi.common.*;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Práctica 1 1C - Chat1.0
 * 
 * @author Miguel Arroyo Pérez
 *
 */
public class ChatClientImpl implements ChatClient {

	private String server;
	private String username;
	private int port;
	private boolean carryOn = true;
	private static int id = 0;
	// private static int clave;

	private static Socket socket = null;

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	/**
	 * Constructor
	 * 
	 * @param server   servidor
	 * @param port     puerto utilizado
	 * @param username nombre del usuario
	 */
	public ChatClientImpl(String server, int port, String username) {
		this.server = server; // IP(127.0.0.1)/nombre de la maquina(localhost)
		this.port = port; // default 1500
		this.username = username; // username/nickname
	}

	/**
	 * Arranca el hilo principal de ejecución del cliente: instancia el cliente y
	 * arranca adicionalmente (en el método start) un hilo adicional a través de
	 * ChatClientListener.
	 * 
	 * @param args arguments (IP username clave)
	 */
	public static void main(String[] args) {
		String server = "localhost";
		String username;
		if (args.length == 1) {
			username = args[0];
		} else {
			server = args[0];
			username = args[1];
		}
		ChatClientImpl cliente = new ChatClientImpl(server, 1500, username);
		System.out.println("CLIENTE: " + username);
		cliente.setUsername(username);
		// clave = Integer.parseInt(args[2]);
		cliente.start(); // Arranca hilo principal
	}

	/**
	 * Arranca un hilo adicional a través de ChatClientListener.
	 */
	public boolean start() {
		BufferedReader in = null;
		PrintWriter out = null;

		try {
			// Connect to Chat Server
			socket = new Socket(server, port);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			id = Integer.parseInt(in.readLine());
			System.out.println("mi id: " + id);
			out.println(getUsername());
			System.out.println("Connected to server " + server + ":" + port);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + server);
			System.exit(1);
		}

		// Arranca hilo adicional para el flujo de entrada
		Thread listener = new Thread(new ChatClientListener(in));
		listener.start();

		// Hilo principal para flujo salida
		try {
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			while (carryOn) {
				String userInput = stdIn.readLine();
				if (userInput.equals("logout")) {
					ChatMessage msg = new ChatMessage(id, MessageType.LOGOUT, userInput);
					sendMessage(msg);
					disconnect();
				} else {
					ChatMessage msg = new ChatMessage(id, MessageType.MESSAGE, userInput);
					sendMessage(msg);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * En el hilo principal se espera a la entrada de consola por parte del usuario
	 * para el envío del mensaje (FLUJO SALIDA). Cuando recibe el mensaje logout, se
	 * deconecta
	 */
	public void sendMessage(ChatMessage msg) {
		PrintWriter out = null;

		// Ciframos el mensaje que se descifrara en el servidor
		// msg.setMessage(cifradoCesar(msg.getMessage(), clave));

		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			//out.print(msg);
			out.println(msg.getMessage());
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + server);
			System.exit(0);
		}
	}

	/**
	 * Implementa el cifrado Cesar a un mensaje con una clave y devuelve el mensaje
	 * cifrado
	 * 
	 * @param msg   mensaje sin cifrar
	 * @param clave clave de cifrado
	 * @return mensaje cifrado
	 */
	/*private static String cifradoCesar(String msg, int clave) {
		String msgCifrado = "";
		char ch;
		// System.out.println("Antes de cifrar con clave " + clave +" :" + msg);
		for (int i = 0; i < msg.length(); i++) {
			ch = msg.charAt(i);
			if (ch >= 'a' && ch <= 'z') {
				ch = (char) (ch + clave);
				if (ch > 'z') {
					ch = (char) (ch - 'z' + 'a' - 1);
				}
				msgCifrado += ch;
			} else if (ch >= 'A' && ch <= 'Z') {
				ch = (char) (ch + clave);
				if (ch > 'Z') {
					ch = (char) (ch - 'Z' + 'A' - 1);
				}
				msgCifrado += ch;
			} else {
				msgCifrado += ch;
			}
		}
		// System.out.println("Despues de cifrar con clave " + clave +" :" +
		// msgCifrado);
		return msgCifrado;
	}*/

	public void disconnect() {
		carryOn = false;
		System.out.println("Me desconecto");
	}

	/**
	 * Implementa la interfaz Runnable, por lo tanto, redefine el método run para
	 * ejecutar el hilo de escucha de mensajes del servidor (flujo de entrada) y
	 * mostrar los mensajes entrantes.
	 * 
	 * @author Miguel Arroyo
	 *
	 */
	private class ChatClientListener implements Runnable {

		private BufferedReader in;

		public ChatClientListener(BufferedReader in) {
			this.in = in;
		}

		public void run() {
			try {
				String message;
				while ((message = in.readLine()) != null) {
					System.out.println("> " + message);
				}
			} catch (IOException e) {
				System.err.println("Couldn't get I/O for the connection to " + server);
				System.exit(1);
			}
		}
	}
}
