package org.infinispan.client.hotrod.query.pressure;

import static org.infinispan.client.hotrod.impl.Util.await;
import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;
import static org.infinispan.query.aggregation.QueryAggregationCountTest.CHUNK_SIZE;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.test.SingleHotRodServerTest;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IsolationLevel;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.aggregation.QueryAggregationCountTest;
import org.infinispan.query.model.Sale;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "org.infinispan.client.hotrod.query.pressure.LargePutAllPressureWithTransactionTest")
public class LargePutAllPressureWithTransactionTest extends SingleHotRodServerTest {

   private final static int SIZE = 15_000;
   private final static int NUMBER_OF_BATCHES = 50;

   private final Random fixedSeedPseudoRandom = new Random(739);

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      ConfigurationBuilder config = new ConfigurationBuilder();
      config.indexing().enable()
            .storage(LOCAL_HEAP)
            .addIndexedEntity("Sale")
            .writer()
            .queueCount(1)
            .queueSize(10_000)
            .locking().isolationLevel(IsolationLevel.REPEATABLE_READ)
            .transaction()
            .transactionManagerLookup(new EmbeddedTransactionManagerLookup())
            .transactionMode(TransactionMode.TRANSACTIONAL)
            .lockingMode(LockingMode.PESSIMISTIC);

      return TestCacheManagerFactory.createServerModeCacheManager(config);
   }

   @Override
   protected org.infinispan.client.hotrod.configuration.ConfigurationBuilder
   createHotRodClientConfigurationBuilder(String host, int serverPort) {
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder =
            super.createHotRodClientConfigurationBuilder(host, serverPort);

      builder.connectionTimeout(Integer.MAX_VALUE);
      builder.socketTimeout(Integer.MAX_VALUE);
      return builder;
   }

   @Override
   protected SerializationContextInitializer contextInitializer() {
      return Sale.SaleSchema.INSTANCE;
   }

   @Test
   public void test() {
      RemoteCache<Object, Object> remoteCache = remoteCacheManager.getCache();
      int days = SIZE / CHUNK_SIZE;
      for (int batch = 0; batch < NUMBER_OF_BATCHES; batch++) {
         HashMap<String, Sale> bulkPut = new HashMap<>(SIZE);
         for (int day = 1; day <= days; day++) {
            bulkPut.putAll(chunkWithUuidKey(day, fixedSeedPseudoRandom));
         }

         CompletableFuture<Void> voidCompletableFuture = remoteCache.putAllAsync(bulkPut);
         await(voidCompletableFuture);
      }

   }

   public static HashMap<String, Sale> chunkWithUuidKey(int day, Random random) {
      HashMap<String, Sale> bulk = new HashMap<>();
      for (int ordinal = 0; ordinal < CHUNK_SIZE; ordinal++) {
         String id = UUID.randomUUID().toString();
         QueryAggregationCountTest.Status status = QueryAggregationCountTest.Status.values()[random.nextInt(QueryAggregationCountTest.Status.values().length)];
         String code = (ordinal % QueryAggregationCountTest.Status.values().length == 0) ? null : UUID.randomUUID().toString();
         Sale sale = new Sale(id, code, status.name(), day);
         bulk.put(sale.getId(), sale);
      }
      return bulk;
   }
}
