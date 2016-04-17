package me.mrkirby153.uhc.bot.cache;

import java.util.HashMap;
import java.util.Map;

public class Cache<K, V> {

    private Map<K, CacheValue<V>> cache = new HashMap<>();

    private final long expireTime;

    public Cache(long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * Stores an object in the cache
     *
     * @param key   The key to store the object under
     * @param value The object to store
     */
    public void put(K key, V value) {
        cache.put(key, new CacheValue<>(value, System.currentTimeMillis() + expireTime));
    }

    /**
     * Gets an object from the cache
     *
     * @param key The key the object is stored under
     * @return The object
     */
    public V get(K key) {
        CacheValue<V> value = cache.get(key);
        if (value == null)
            return null;
        if (value.expired()) {
            cache.remove(key);
            return null;
        }
        return value.getValue();
    }

    /**
     * Removes an object from the cache
     *
     * @param key The key to remove
     * @return The object that was removed
     */
    public V remove(K key) {
        V value = get(key);
        cache.remove(key);
        return value;
    }

    /**
     * Checks if the given key is in the cache
     * @param key The key to check
     * @return True if the key is in the map, false if it isn't
     */
    public boolean containsKey(K key){
        return cache.containsKey(key);
    }

    /**
     * Checks if the given value is in the cache
     * @param value The value to check
     * @return True if the value is in the map, false if it isn't
     */
    public boolean containsvalue(V value){
        return cache.values().stream().filter(val -> val.getValue().equals(value)).count() > 0;
    }

    /**
     * Gets the raw cache
     *
     * @return The cache
     */
    protected Map<K, CacheValue<V>> getCache() {
        return cache;
    }

    /**
     * Adds a {@link CacheValue} directly to the cache
     *
     * @param key   The key to store it under
     * @param value The value to store
     */
    protected void put(K key, CacheValue<V> value) {
        cache.put(key, value);
    }

    /**
     * Gets the expire time (how long objects will persist in the cache)
     *
     * @return The expire time in milliseconds
     */
    public long getExpireTime() {
        return expireTime;
    }

    protected static class CacheValue<V> {
        private final V value;
        private final long expiresOn;

        public CacheValue(V value, long expiresOn) {
            this.value = value;
            this.expiresOn = expiresOn;
        }

        public V getValue() {
            return value;
        }

        public long getExpiresOn() {
            return expiresOn;
        }

        public boolean expired() {
            return System.currentTimeMillis() > expiresOn;
        }
    }
}
