package local.example.deployableticketsystem.service;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Reservation;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.ReservationRepository;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {

  private final TicketRepository ticketRepository;
  private final ReservationRepository reservationRepository;
  private final RedissonClient redisson;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public TicketService(TicketRepository ticketRepository,
      ReservationRepository reservationRepository,
      RedissonClient redisson,
      PlatformTransactionManager transactionManager) {
    this.ticketRepository = ticketRepository;
    this.reservationRepository = reservationRepository;
    this.redisson = redisson;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public void reserve(UUID ticketId, String userId) {
    RAtomicLong stock = redisson.getAtomicLong("TICKET" + ticketId);
    long remaining = stock.decrementAndGet();
    if (remaining < 0) {
      stock.incrementAndGet();
      throw new IllegalStateException("Sold out");
    }

    RDeque<String> queue = redisson.getDeque("reservation_queue");
    queue.addLast(ticketId + ":" + userId);
  }

  @Scheduled(fixedDelay = 500)
  public void consumeReservationQueue() {
    RDeque<String> queue = redisson.getDeque("reservation_queue");
    String item;
    while ((item = queue.pollFirst()) != null) {
      String[] parts = item.split(":");
      UUID ticketId = UUID.fromString(parts[0]);
      String userId = parts[1];

      try {
        transactionTemplate.executeWithoutResult(status -> {
          Ticket ticket = ticketRepository.getReferenceById(ticketId);
          reservationRepository.save(new Reservation(userId, ticket));
          ticketRepository.decreaseStock(ticketId);
        });
      } catch (Exception e) {
        System.err.println("Failed to save reservation: " + item);
        e.printStackTrace();
      }
    }
  }
}
