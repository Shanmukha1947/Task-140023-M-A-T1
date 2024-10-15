import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.TimeUnit;

public class CaffeineWithElastiCacheExample {

    public static void main(String[] args) {
        // Step 1: Set up AWS ElastiCache
        final String endpoint = "your-elasticache-endpoint.amazonaws.com"; // Replace with your ElastiCache endpoint
        final String port = "6379"; // Default Redis port

        // Create a JedisPool to connect to ElastiCache
        JedisPool jedisPool = createJedisPool(endpoint, port);

        // Step 2: Create a Caffeine cache backed by ElastiCache
        Cache<String, String> caffeineCache = createCaffeineCacheWithElastiCache(jedisPool);

        // Step 3: Use the Caffeine cache
        testCaffeineCache(caffeineCache);

        jedisPool.close();
    }

    private static JedisPool createJedisPool(String endpoint, String port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);

        return new JedisPool(poolConfig, endpoint, Integer.parseInt(port));
    }

    private static Cache<String, String> createCaffeineCacheWithElastiCache(JedisPool jedisPool) {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .removalListener((key, value, cause) -> {
                    // Optionally, handle removal events here
                })
                .build(key -> {
                    // Load data from a persistent store (e.g., database) when a key is missing in cache
                    // For demonstration, we'll just return the key as the value.
                    try (Jedis jedis = jedisPool.getResource()) {
                        return jedis.get(key);
                    }
                });
    }

    private static void testCaffeineCache(Cache<String, String> caffeineCache) {
        String key = "userId-123";
        String value = "User Data for UserId-123";

        // Put value in cache
        caffeineCache.put(key, value);

        // Get value from cache
        String cachedValue = caffeineCache.getIfPresent(key);
        System.out.println("Cached Value: " + cachedValue);

        // Update value in cache
        caffeineCache.put(key, "Updated User Data");
        cachedValue = caffeineCache.getIfPresent(key);
        System.out.println("Updated Cached Value: " + cachedValue);

        // Invalidate cache entry
        caffeineCache.invalidate(key);
        cachedValue = caffeineCache.getIfPresent(key);
        System.out.println("Cached Value after Invalidation: " + cachedValue);
    }
}
