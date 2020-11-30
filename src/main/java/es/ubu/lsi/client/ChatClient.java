package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Define la signatura de los métodos de arranque, envío de mensaje y
 * desconexión.
 * 
 * @author Miguel Arroyo Pérez
 *
 */
public interface ChatClient {

	public boolean start();

	public void sendMessage(ChatMessage msg);

	public void disconnect();
}
