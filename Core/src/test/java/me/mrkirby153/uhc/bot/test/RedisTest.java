package me.mrkirby153.uhc.bot.test;

import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import me.mrkirby153.uhc.bot.network.data.RedisDataStore;
import me.mrkirby153.uhc.bot.network.data.Utility;
import redis.clients.jedis.JedisPool;

public class RedisTest {

    protected RedisConnection connection;
    protected RedisDataStore<Utilities.TestData> dataStore;
    protected JedisPool jedisPool;


    public final void setupJedis(){
        connection = Utilities.getConnection();
        jedisPool = Utility.createPool(connection);
        dataStore = new RedisDataStore<>(Utilities.TestData.class, "test-data", connection);
    }

    public final void cleanUp(){
        if(dataStore != null) {
            dataStore.getElements().forEach(dataStore::removeElement);
            dataStore.getDeadElements().forEach(dataStore::removeElement);
        }
        if(jedisPool != null)
            jedisPool.close();
    }

    public final Utilities.TestData getTestData(){
        return new Utilities.TestData("testing", "testing");
    }

    public final Utilities.TestData populateTestData(){
        Utilities.TestData testData = getTestData();
        dataStore.addElement(testData);
        return testData;
    }
}
