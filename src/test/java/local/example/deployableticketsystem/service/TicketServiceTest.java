package local.example.deployableticketsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import local.example.deployableticketsystem.entity.Event;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.EventRepository;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class TicketServiceTest {

  @Autowired
  private TicketService ticketService;

  @Autowired
  private TicketRepository ticketRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private RedissonClient redisson;

  @Test
  void reserve_withConcurrentRequests_shouldReturnRemaining0() {
    // given
    long threadCount = 10000L;

    Event event = new Event("test event title", "test event desc", "test event loc", Instant.now());
    eventRepository.save(event);

    Ticket ticket = ticketRepository.save(new Ticket(event, "R", 1000L, threadCount));
    UUID ticketId = ticket.getId();

    RAtomicLong stock = redisson.getAtomicLong("TICKET" + ticketId);
    stock.set(threadCount);

    // when
    try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
      CountDownLatch latch = new CountDownLatch((int) threadCount);
      for (long i = 0; i < threadCount; i++) {
        String userId = "User-" + i;
        executorService.submit(() -> {
          try {
            ticketService.reserve(ticketId, userId);
          } finally {
            latch.countDown();
          }
        });
      }
      latch.await();
    } catch (Exception e) {
      e.printStackTrace();
    }

    RDeque<String> queue = redisson.getDeque("reservation_queue");
    assertEquals(threadCount, queue.size());

    Ticket intermediateTicket = ticketRepository.findById(ticketId).orElseThrow();
    assertEquals(threadCount, intermediateTicket.getRemainingQuantity());

    ticketService.consumeReservationQueue();

    // then
    assertEquals(0, stock.get());
    Ticket resultTicket = ticketRepository.findById(ticketId).orElseThrow();
    assertEquals(0, resultTicket.getRemainingQuantity());
  }
}
