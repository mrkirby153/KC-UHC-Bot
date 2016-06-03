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

public class NetworkConnection extends Thread {

    private final Socket socket;

    private boolean running = true;
    private InputStream inputStream;
    private OutputStream outputStream;

    public NetworkConnection(Socket socket) {
        this.socket = socket;
        setDaemon(true);
        setName("NetworkConnection (" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")");
    }

    @Override
    public void run() {
        Main.logger.info("Handling connection for " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        while (running) {
            if (socket.isClosed()) {
                running = false;
                return;
            }
            try {
                this.inputStream = socket.getInputStream();
                this.outputStream = socket.getOutputStream();
                if (inputStream == null)
                    continue;
                byte[] rawMessageSize = new byte[4];
                if (inputStream.read(rawMessageSize) <= 0) continue;
                ByteBuffer msgLenBuff = ByteBuffer.wrap(rawMessageSize);
                msgLenBuff.order(ByteOrder.LITTLE_ENDIAN);
                msgLenBuff.rewind();
                byte[] rawMessage = new byte[msgLenBuff.getInt()];
                if (inputStream.read(rawMessage) <= 0) {
                    System.out.println("Read no bytes!");
                    continue;
                }
                ByteArrayDataInput in = ByteStreams.newDataInput(rawMessage);
                String serverId = in.readUTF();
                String command = in.readUTF();
                ByteArrayDataOutput out = CommandHandler.execute(serverId, command, in);
                sendMessage(out.toByteArray());
            } catch (SocketException e) {
                if (!e.getMessage().equalsIgnoreCase("connection reset") && !e.getMessage().equalsIgnoreCase("socket closed")) {
                    e.printStackTrace();
                } else {
                    Main.logger.info("Lost connection from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                }
                running = false;
            } catch (Exception e) {
                Main.logger.info("An unknown exception occurred!");
                sendMessage(new byte[0]);
                e.printStackTrace();
            }
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
}
