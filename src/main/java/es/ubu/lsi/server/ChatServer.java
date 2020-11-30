package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Define la signatura de los métodos de arranque, multidifusión, eliminación de
 * cliente y apagado.
 * 
 * @author Miguel Arroyo
 *
 */
public interface ChatServer {

	public void startup();

	public void shutdown();

	public void broadcast(ChatMessage msg);

	public void remove(int id);
}
