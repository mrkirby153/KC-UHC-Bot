package me.mrkirby153.uhc.bot.cache;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.uhc.bot.Main;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class PersistentFileCache extends Cache<String, byte[]> {

    public PersistentFileCache(long expireTime) {
        super(expireTime);
    }

    public PersistentFileCache() {
        this(86400000);
    }


    /**
     * Gets the string stored in the cache
     *
     * @param key The key it's stored under by
     * @return A string representation of the data
     */
    public String getString(String key) {
        return new String(super.get(key));
    }

    /**
     * Saves a string in the cache
     *
     * @param key   The key to store it under
     * @param value The value to store
     */
    public void putString(String key, String value) {
        super.put(key, value.getBytes());
    }

    /**
     * Writes the file cache to a file
     *
     * @param file The file to write
     */
    public void saveToFile(File file) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        Map<String, CacheValue<byte[]>> cache = getCache();
        out.writeLong(getExpireTime());
        out.writeInt(cache.size());
        for (Map.Entry<String, CacheValue<byte[]>> m : cache.entrySet()) {
            out.writeUTF(m.getKey());
            CacheValue<byte[]> v = m.getValue();
            out.writeInt(v.getValue().length);
            out.writeLong(v.getExpiresOn());
            out.write(v.getValue());
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(out.toByteArray());
            fos.close();
        } catch (IOException e) {
            Main.logger.catching(Level.FATAL, e);
            Main.logger.fatal("Error saving persistent file cache!");
        }
    }

    /**
     * Loads a file cache from the disk
     *
     * @param file     The file to load from
     * @return The file cache
     */
    public static PersistentFileCache loadFromFile(File file) {
        try {
            if (!file.exists()) {
                return null;
            }
            ByteArrayDataInput in = ByteStreams.newDataInput(Files.readAllBytes(file.toPath()));
            PersistentFileCache pfc = new PersistentFileCache(in.readLong());
            int cacheSize = in.readInt();
            for (int i = 0; i < cacheSize; i++) {
                String key = in.readUTF();
                byte[] data = new byte[in.readInt()];
                long expiresOn = in.readLong();
                for (int j = 0; j < data.length; j++) {
                    data[j] = in.readByte();
                }
                CacheValue<byte[]> s = new CacheValue<>(data, expiresOn);
                pfc.put(key, s);
            }
            return pfc;
        } catch (Exception e) {
            Main.logger.catching(Level.FATAL, e);
            Main.logger.fatal("Error loading persistent file cache!");
        }
        return null;
    }
}
