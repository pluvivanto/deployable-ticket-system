package local.example.deployableticketsystem.component;

import java.util.List;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisStockInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(RedisStockInitializer.class);

  private final TicketRepository ticketRepository;
  private final RedissonClient redissonClient;

  @Autowired
  public RedisStockInitializer(TicketRepository ticketRepository, RedissonClient redissonClient) {
    this.ticketRepository = ticketRepository;
    this.redissonClient = redissonClient;
  }

  @Override
  public void run(String... args) throws Exception {
    var lock = redissonClient.getLock("stock:initialization:lock");
    org.redisson.api.RBucket<Boolean> completionFlag = redissonClient.getBucket(
        "stock:initialization:complete");

    if (lock.tryLock(60, 60, java.util.concurrent.TimeUnit.SECONDS)) {
      try {
        if (completionFlag.isExists() && completionFlag.get()) {
          log.info("Stock already initialized by another instance. Skipping.");
          return;
        }

        log.info("Acquired initialization lock. Starting Redis stock sync...");
        List<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
          String key = "TICKET" + ticket.getId();
          RAtomicLong stock = redissonClient.getAtomicLong(key);
          stock.set(ticket.getRemainingQuantity());
        }

        // Mark as complete so subsequent lock-holders skip
        completionFlag.set(true);
        log.info("Redis stock initialization complete.");
      } finally {
        if (lock.isHeldByCurrentThread()) {
          lock.unlock();
        }
      }
    } else {
      throw new IllegalStateException("Could not acquire lock to verify stock initialization.");
    }
  }
}
