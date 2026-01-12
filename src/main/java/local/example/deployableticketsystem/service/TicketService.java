package local.example.deployableticketsystem.service;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Reservation;
import local.example.deployableticketsystem.entity.ReservationStatusEnum;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.ReservationRepository;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);
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

  public void syncStock(UUID ticketId, Long quantity) {
    RAtomicLong stock = redisson.getAtomicLong("TICKET" + ticketId);
    if (!stock.isExists()) {
      stock.set(quantity);
      log.info("Synced stock for ticket {} to {}", ticketId, quantity);
    } else {
      log.debug("Stock for ticket {} already exists in Redis. Skipping sync.", ticketId);
    }
  }

  @Scheduled(fixedDelay = 500)
  public void consumeReservationQueue() {
    RDeque<String> queue = redisson.getDeque("reservation_queue");
    String item;
    while ((item = queue.pollFirst()) != null) {
      try {
        String[] parts = item.split(":");
        if (parts.length < 2) {
          throw new IllegalArgumentException("Invalid message format");
        }
        UUID ticketId = UUID.fromString(parts[0]);
        String userId = parts[1];
        transactionTemplate.executeWithoutResult(status -> {
          Ticket ticket = ticketRepository.getReferenceById(ticketId);
          Reservation reservation = new Reservation(userId, ticket);
          reservation.setStatus(ReservationStatusEnum.CONFIRMED);
          reservationRepository.save(reservation);
          ticketRepository.decreaseStock(ticketId);
        });
      } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        log.error("Invalid reservation request format: {}. Discarding.", item, e);
        // Do not re-queue
      } catch (Exception e) {
        log.error("Failed to process reservation: {}. Moving to tail and pausing.", item, e);
        queue.addLast(item);
        break;
      }
    }
  }
}
