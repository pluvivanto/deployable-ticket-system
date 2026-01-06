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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TicketServiceTest {

  @Autowired
  private TicketService ticketService;

  @Autowired
  private TicketRepository ticketRepository;

  @Autowired
  private EventRepository eventRepository;

  @Test
  void reserve_withConcurrentRequests_shouldReturnRemaining0() {
    // given
    int threadCount = 1000;

    Event event = new Event();
    event.setTitle("test event title");
    event.setDescription("test event desc");
    event.setLocation("test event loc");
    event.setOpenAt(Instant.now());
    eventRepository.save(event);

    Ticket ticket = new Ticket(event, "R", 1000L, threadCount);
    Ticket savedTicket = ticketRepository.save(ticket);
    UUID savedTicketId = savedTicket.getId();

    // when
    try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
      CountDownLatch latch = new CountDownLatch(threadCount);
      for (int i = 0; i < threadCount; i++) {
        String userId = "User-" + i;
        executorService.submit(() -> {
          try {
            ticketService.reserve(savedTicketId, userId);
          } finally {
            latch.countDown();
          }
        });
      }
      latch.await();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // then
    Ticket resultTicket = ticketRepository.findById(savedTicketId).orElseThrow();
    System.out.println("remaining: " + resultTicket.getRemainingQuantity());
    assertEquals(0, resultTicket.getRemainingQuantity());
  }
}
