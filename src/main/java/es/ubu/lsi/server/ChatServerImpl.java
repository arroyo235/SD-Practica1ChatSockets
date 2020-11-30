package es.ubu.lsi.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Por defecto el servidor se ejecuta en el puerto 1500. en su invocación no
 * recibe argumentos.
 * 
 * @author Miguel Arroyo
 *
 */
public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
	private static int clientId = 0;
	private static SimpleDateFormat sdf;
	private int port;
	private boolean alive;

	/**
	 * Alamcenamoos el socket del cliente, su nombre de usuario y la hora en la que
	 * se conecta.
	 */
	private static ArrayList<Object> clientInfo;

	/**
	 * Registro de los clientes y su informacion
	 */
	private Map<Integer, ArrayList<Object>> clients = new HashMap<Integer, ArrayList<Object>>();

	/**
	 * Constructor
	 * 
	 * @param port puerto
	 */
	public ChatServerImpl(int port) {
		this.port = port;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	/**
	 * Arranca el hilo principal de ejecución del servidor: instancia el servidor y
	 * arranca (en el método startup) un hilo adicional a través de
	 * ChatClientListener.
	 * 
	 * @param args arguments
	 */
	public static void main(String[] args) {
		System.out.println("Server is running on port " + DEFAULT_PORT);
		ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
		server.startup();
	}

	/**
	 * Implementa el bucle con el servidor de sockets (ServerSocket), esperando y
	 * aceptado peticiones. Ante cada petición entrante y aceptada, se instancia un
	 * nuevo ServerThreadForClient y se arranca el hilo correspondiente para que
	 * cada cliente tenga su hilo independiente asociado en el servidor (con su
	 * socket, flujo de entrada y flujo de salida).
	 * 
	 * Es importante ir guardando un registro de los hilos creados para poder
	 * posteriormente realizar el push de los mensajes y un apagado correcto.
	 */
	public void startup() {

		Socket clientSocket = null;
		BufferedReader in = null;
		PrintWriter out = null;
		String username = "";
		while (true) {

			try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);) {
				clientId++;

				/* Waits for a connection */
				clientSocket = serverSocket.accept();

				Date date = new Date();
				sdf = new SimpleDateFormat("HH:mm:ss");
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out = new PrintWriter(clientSocket.getOutputStream(), true);

				out.println(clientId);
				username = in.readLine();

				System.out.println(
						" >> " + "Client " + username + " No:" + clientId + " started at: " + sdf.format(date));

				/* Adds the client into a map(id, clientInfo(socket, username, date) */
				clientInfo = new ArrayList<Object>();
				clientInfo.add(0, clientSocket);
				clientInfo.add(1, username);
				clientInfo.add(2, sdf.format(date));
				clients.put(clientId, clientInfo);

			} catch (IOException e) {
				System.out.println("Exception caught when trying to listen on port " + DEFAULT_PORT
						+ " or listening for a connection");
				System.out.println(e.getMessage());
			}

			ServerThreadForClient threadForClient = new ServerThreadForClient(clientSocket, clientId, username);
			threadForClient.start();
		}
	}

	/**
	 * Método no implementado
	 */
	public void shutdown() {
		setAlive(false);
	}

	/**
	 * Envía el mensaje recepcionado a todos los clientes (flujo de salida).
	 */
	public void broadcast(ChatMessage msg) {

		// recorrer lista de todos los clientes activos y envia
		// el mensaje a todos menos a quien lo envia
		PrintWriter out = null;
		try {

			for (Map.Entry<Integer, ArrayList<Object>> entry : clients.entrySet()) {
				String message = msg.getId()+ ": " + msg.getMessage();
				
				if (msg.getId() != entry.getKey()) {
					Socket cs = (Socket) entry.getValue().get(0);
					out = new PrintWriter(cs.getOutputStream(), true);
					out.println(message);
					// System.out.println("Se envia mensaje del cliente "+ msg.getId() +" al cliente
					// " + entry.getKey());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Elimina un cliente de la lista.
	 */
	public void remove(int id) {
		System.out.println("Removed the client " + id);
		clients.remove(id);
	}

	/**
	 * El cliente podrá conocer quiénes son los clientes actualmente conectados al
	 * chat y a qué hora se conectaron, recibiendo, por ejemplo, un mensaje del
	 * servidor similar a este:
	 * 
	 * Pepe (14:25), Pio (16:30), Blas (15:12)
	 * 
	 * Se excluirá de este mensaje de respuesta, el propio usuario que hace la
	 * petición.
	 * 
	 * @param username
	 */
	public void seeConnUsers(ChatMessage msg, String username) {
		// System.out.println("Viendo los usuarios conectados: ");
		PrintWriter out = null;
		String message = "";

		for (Map.Entry<Integer, ArrayList<Object>> entry : clients.entrySet()) {
			if (msg.getId() != entry.getKey()) {
				message = message + entry.getValue().get(1) + " (" + entry.getValue().get(2) + "), ";
			}
		}

		try {
			for (Map.Entry<Integer, ArrayList<Object>> entry : clients.entrySet()) {
				if (msg.getId() == entry.getKey()) {
					Socket cs = (Socket) entry.getValue().get(0);
					out = new PrintWriter(cs.getOutputStream(), true);
					out.println(message);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Implementa el cifrado Cesar a un mensaje con una clave y devuelve el mensaje
	 * descifrado
	 * 
	 * @param msg   mensaje cifrado
	 * @param clave clave para descifrar
	 * @return mensaje descifrado
	 */
	/*private String descrifrarCesar(String msg, int clave) {
		String msgDescifrado = "";
		char ch;
		// System.out.println("Antes de cifrar con clave " + clave +" :" + msg);
		for (int i = 0; i < msg.length(); i++) {
			ch = msg.charAt(i);
			if (ch >= 'a' && ch <= 'z') {
				ch = (char) (ch - clave);
				if (ch < 'a') {
					ch = (char) (ch + 'z' - 'a' + 1);
				}
				msgDescifrado += ch;
			} else if (ch >= 'A' && ch <= 'Z') {
				ch = (char) (ch - clave);
				if (ch < 'A') {
					ch = (char) (ch + 'Z' - 'A' + 1);
				}
				msgDescifrado += ch;
			} else {
				msgDescifrado += ch;
			}
		}
		return msgDescifrado;
	}*/

	private class ServerThreadForClient extends Thread {
		private int id;
		private String username;

		protected Socket cs;
		protected ChatMessage msg = null;

		public ServerThreadForClient(Socket clientSocket, int clientId, String username) {
			this.cs = clientSocket;
			this.id = clientId;
			this.username = username;
		}

		/**
		 * Espera en un bucle a los mensajes recibidos de cada cliente (flujo de
		 * entrada), realizándose la operación correspondiente (a través de los métodos
		 * de la clase externa, ChatServer). A la finalización de su ejecución se debe
		 * eliminar al propio cliente de la lista de clientes activos
		 */
		public void run() {
			// El metedo run espera en un blucle a los mensajes recibidos de cada cliente
			// (flujo de entrada)

			while (true) {
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(cs.getInputStream()));
					//ChatMessage msg = null;
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						if (inputLine.equals("logout")) {
							// Se quita al usuario de la lista de conectados
							remove(id);
						} else if (inputLine.equals("users")) {
							// Muestra los usurios conectados
							msg = new ChatMessage(id, MessageType.MESSAGE, inputLine);
							seeConnUsers(msg, username);
						} else {
							msg = new ChatMessage(id, MessageType.MESSAGE, inputLine);
							broadcast(msg);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}
}