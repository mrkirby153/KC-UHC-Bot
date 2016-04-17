package me.mrkirby153.uhc.bot.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.uhc.bot.Main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class NetworkConnection extends Thread {

    private final Socket socket;

    private boolean running = true;
    private InputStream inputStream;
    private OutputStream outputStream;
    private KeyPair ourKey;
    private PublicKey theirPubKey;
    private State state = State.INITLAILIZED;

    public NetworkConnection(Socket socket) {
        this.socket = socket;
        setDaemon(true);
        ourKey = NetworkHandler.Cryptography.generateKeypair(2048);
        state = State.HANDSHAKING;
        setName("NetworkConnection");
    }

    @Override
    public void run() {
        Main.logger.info("Handling connection for " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        try {
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            while (running) {
                if (socket.isClosed()) {
                    running = false;
                    continue;
                }
                if (state == State.HANDSHAKING) {
                    // Begin the handshake
                    // Read their pubkey
                    byte[] theirPubKey = new byte[2048];
                    inputStream.read(theirPubKey);
                    this.theirPubKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(theirPubKey));
                    // Write our pubkey
                    outputStream.write(ourKey.getPublic().getEncoded());
                    outputStream.flush();
                    state = State.RUNNING;
                }
                if (state == State.RUNNING) {
                    if (inputStream == null)
                        continue;
                    byte[] messageSizeBytes = new byte[4];
                    if (inputStream.read(messageSizeBytes) <= 0) continue;
                    ByteBuffer msgLenBuff = ByteBuffer.wrap(messageSizeBytes);
                    msgLenBuff.order(ByteOrder.LITTLE_ENDIAN);
                    msgLenBuff.rewind();
                    byte[] encodedMessage = new byte[msgLenBuff.getInt()];
                    inputStream.read(encodedMessage);
                    ByteBuffer msgBuff = ByteBuffer.wrap(encodedMessage);
                    msgBuff.rewind();
                    byte[] rawDecrypted = NetworkHandler.Cryptography.decrypt(ourKey.getPrivate(), encodedMessage);
                    System.out.println("Message size: "+encodedMessage.length);
                    if (rawDecrypted == null) {
                        Main.logger.warn("Decryption failed!");
                        continue;
                    }
                    ByteArrayDataInput in = ByteStreams.newDataInput(rawDecrypted);
                    String serverId = in.readUTF();
                    String command = in.readUTF();
                    ByteArrayDataOutput out = CommandHandler.execute(serverId, command, in);
                    sendMessage(NetworkHandler.Cryptography.encrypt(theirPubKey,out.toByteArray()));
                }
            }
        } catch (SocketException e) {
            if (!e.getMessage().equalsIgnoreCase("connection reset") && !e.getMessage().equalsIgnoreCase("socket closed")) {
                e.printStackTrace();
            } else {
                Main.logger.info("Lost connection from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(NetworkHandler.Cryptography.encrypt(theirPubKey, new byte[0]));
            run();
        }
    }

    protected void shutdown() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message) {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        buff.putInt(message.length);
        buff.rewind();
        try {
            outputStream.write(buff.array());
            outputStream.write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum State {
        INITLAILIZED,
        HANDSHAKING,
        RUNNING,
        CLOSED;
    }
}
