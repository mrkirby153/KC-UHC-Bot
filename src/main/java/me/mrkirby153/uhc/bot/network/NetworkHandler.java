package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.Main;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.List;

public class NetworkHandler extends Thread {

    private final int port;

    private ServerSocket socket;

    private List<NetworkConnection> connections = new ArrayList<>();

    private boolean running = true;

    public NetworkHandler(int port) {
        this.port = port;
        setName("NetworkHandler");
        setDaemon(true);
    }

    @Override
    public void run() {
        Main.logger.info("Listening on port " + port);
        try {
            socket = new ServerSocket(this.port);
            while (running) {
                Socket s = socket.accept();
                NetworkConnection n = new NetworkConnection(s);
                n.start();
                connections.add(n);
                Main.logger.info("Connect from " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
            }
        } catch (SocketException e) {
            if (!e.getMessage().equalsIgnoreCase("socket closed"))
                e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void closeConnections() {
        connections.forEach(NetworkConnection::shutdown);
    }

    public void shutdown() {
        running = false;
        closeConnections();
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class Cryptography {

        public static KeyPair generateKeypair(int bits) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits, RSAKeyGenParameterSpec.F4);
                kpg.initialize(spec);
                return kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static byte[] decrypt(PrivateKey privateKey, byte[] message) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                return cipher.doFinal(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public static byte[] encrypt(PublicKey publicKey, byte[] message) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                return cipher.doFinal(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
