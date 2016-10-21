package me.mrkirby153.uhc.bot.network.data;

public class RedisConnection {
    private final String host;
    private final String password;
    private final int port;

    public RedisConnection(String host, int port, String password){
        this.host = host;
        this.port = port;
        this.password = password;
    }

    /**
     * Gets the host of the redis server
     * @return The redis host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the password required for the redis server
     * @return The password required
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the port of the redis server
     * @return The port of the server
     */
    public int getPort() {
        return port;
    }
}
