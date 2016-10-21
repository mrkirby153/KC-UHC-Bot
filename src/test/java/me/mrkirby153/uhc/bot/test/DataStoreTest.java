package me.mrkirby153.uhc.bot.test;

import org.junit.Assert;
import org.junit.Test;

public class DataStoreTest extends RedisTest{

    @Test
    public void testStoringAndRetrieval() throws Exception {
        setupJedis();
        Utilities.TestData data = populateTestData();

        Assert.assertNotNull(dataStore.getElement(data.getIdentifier()));
        Utilities.TestData d1 = dataStore.getElement(data.getIdentifier());
        Assert.assertEquals(data.getKey(), d1.getKey());
        Assert.assertEquals(data.getValue(), d1.getValue());

        cleanUp();
    }

    @Test
    public void testExpirey() throws Exception {
        setupJedis();

        Utilities.TestData data = getTestData();
        dataStore.addElement(data, 1500);
        Assert.assertNotNull(dataStore.getElement(data));
        Thread.sleep(1500);
        Assert.assertNull(dataStore.getElement(data));

        cleanUp();
    }

    @Test
    public void testUpdate() throws Exception{
        setupJedis();
        Utilities.TestData data = populateTestData();

        Assert.assertEquals("testing", dataStore.getElement(data).getValue());

        Utilities.TestData d1 = new Utilities.TestData("testing", "12345");
        dataStore.updateElement(data.getIdentifier(), d1);

        Assert.assertEquals("12345", dataStore.getElement(data).getValue());

        cleanUp();
    }
}
