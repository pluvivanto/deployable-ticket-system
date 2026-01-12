package local.example.deployableticketsystem.component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.TicketRepository;
import local.example.deployableticketsystem.service.TicketService;
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
  private final TicketService ticketService;

  @Autowired
  public RedisStockInitializer(TicketRepository ticketRepository, RedissonClient redissonClient,
      TicketService ticketService) {
    this.ticketRepository = ticketRepository;
    this.redissonClient = redissonClient;
    this.ticketService = ticketService;
  }

  @Override
  public void run(String... args) throws Exception {
    var lock = redissonClient.getLock("stock:initialization:lock");
    org.redisson.api.RBucket<Boolean> completionFlag = redissonClient.getBucket(
        "stock:initialization:complete");

    if (lock.tryLock(60, 60, TimeUnit.SECONDS)) {
      try {
        if (completionFlag.isExists() && completionFlag.get()) {
          log.info("Stock already initialized by another instance. Skipping.");
          return;
        }

        log.info("Acquired initialization lock. Starting Redis stock sync...");
        List<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
          ticketService.syncStock(ticket.getId(), ticket.getRemainingQuantity());
        }

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
