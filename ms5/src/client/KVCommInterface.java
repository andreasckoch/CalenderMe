package client;

public interface KVCommInterface {

    /**
     * Establishes a connection to the KV Server.
     *
     * @throws Exception if connection could not be established.
     */
    public void connect() throws Exception;

    /**
     * disconnects the client from the currently connected server.
     */
    public void disconnect();

}
