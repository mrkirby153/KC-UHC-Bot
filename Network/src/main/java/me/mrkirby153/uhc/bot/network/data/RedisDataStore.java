package me.mrkirby153.uhc.bot.network.data;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisDataStore<T extends RedisData> implements Iterable<T> {

    private final Class<T> objectClass;
    private final String label;

    private final JedisPool pool;

    public RedisDataStore(Class<T> clazz, String label, RedisConnection connection) {
        this.objectClass = clazz;
        this.label = label;
        this.pool = Utility.createPool(connection);

    }

    public void addElement(T element, int expire) {
        if (elementExists(element.getIdentifier())) {
            removeElement(element.getIdentifier());
        }
        try (Jedis j = pool.getResource()) {
            String serialized = Utility.serialize(element);
            String key = generateKey(element.getIdentifier());
            long expires = Utility.currentTime(pool) + expire;
            Transaction t = j.multi();
            t.set(key, serialized);
            t.zadd(this.label, expires, element.getIdentifier());
            t.exec();
        }
    }

    public void addElement(T element) {
        addElement(element, 1000 * 60 * 60 * 24); // Default to 24 hours
    }

    public boolean elementExists(String id) {
        try (Jedis j = pool.getResource()) {
            if (getDeadElements().contains(generateKey(id))) {
                removeElement(id);
                return false;
            }
            return j.exists(generateKey(id));
        }
    }

    /**
     * Gets all the elements that expired
     *
     * @return All the elements that expired
     */
    public Set<String> getDeadElements() {
        try (Jedis j = pool.getResource()) {
            return j.zrangeByScore(label, "-inf", Long.toString(Utility.currentTime(pool)));
        }
    }

    public T getElement(String elementId) {
        try (Jedis j = pool.getResource()) {
            String key = generateKey(elementId);
            String serialized = j.get(key);
            if(getDeadElements().contains(elementId)){
                getDeadElements().forEach(this::removeElement);
                return null;
            }
            return Utility.deserialize(serialized, objectClass);
        }
    }

    public T getElement(T element){
        return getElement(element.getIdentifier());
    }

    public Collection<T> getElements() {
        return getElements(getAliveElements());
    }

    public Collection<T> getElements(Collection<String> ids) {
        return ids.stream().map(this::getElement).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Iterator<T> iterator() {
        return getElements().iterator();
    }

    public void removeElement(T element) {
        removeElement(element.getIdentifier());
    }

    public void removeElement(String dataId) {
        String key = generateKey(dataId);
        try (Jedis j = pool.getResource()) {
            j.del(key);
            j.zrem(this.label, dataId);
        }
    }

    public void updateElement(String dataId, T newElement) {
        try (Jedis j = pool.getResource()) {
            String serialized = Utility.serialize(newElement);
            String key = generateKey(dataId);
            j.set(key, serialized);
        }
    }

    private String generateKey(String id) {
        return String.format("data:%s:%s", this.label, id);
    }

    private Set<String> getAliveElements() {
        try (Jedis j = pool.getResource()) {
            return j.zrangeByScore(label, "(" + Utility.currentTime(pool), "+inf");
        }
    }
}
