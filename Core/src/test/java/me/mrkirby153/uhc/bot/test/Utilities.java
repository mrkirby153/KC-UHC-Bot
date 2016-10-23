package me.mrkirby153.uhc.bot.test;


import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import me.mrkirby153.uhc.bot.network.data.RedisData;

public class Utilities {

    public static RedisConnection getConnection() {
        return new RedisConnection(getEnvironmentVariable("REDIS_HOST", "localhost"), Integer.parseInt(getEnvironmentVariable("REDIS_PORT", "6379")),
                getEnvironmentVariable("REDIS_PW", null));
    }

    public static String getEnvironmentVariable(String environmentVariable, String def) {
        String env = System.getenv(environmentVariable);
        if (env == null) {
            return def;
        } else {
            return env;
        }
    }

    public static class TestData implements RedisData {

        private final String key, value;

        public TestData(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getIdentifier() {
            return key;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
