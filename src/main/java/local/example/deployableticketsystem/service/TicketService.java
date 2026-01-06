package local.example.deployableticketsystem.service;

import jakarta.transaction.Transactional;
import java.util.UUID;
import local.example.deployableticketsystem.entity.Reservation;
import local.example.deployableticketsystem.entity.ReservationStatusEnum;
import local.example.deployableticketsystem.entity.Ticket;
import local.example.deployableticketsystem.repository.ReservationRepository;
import local.example.deployableticketsystem.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

  private final TicketRepository ticketRepository;
  private final ReservationRepository reservationRepository;

  @Autowired
  public TicketService(TicketRepository ticketRepository,
      ReservationRepository reservationRepository) {
    this.ticketRepository = ticketRepository;
    this.reservationRepository = reservationRepository;
  }

  @Transactional
  public Boolean reserve(UUID ticketId, String userId) {
    Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Ticket not found"));
    Integer remainingQuantity = ticket.getRemainingQuantity();
    if (remainingQuantity <= 0) {
      System.err.println("There isn't any remaining ticket for " + ticketId);
      return false;
    }
    Reservation reservation = new Reservation();
    reservation.setUserId(userId);
    reservation.setTicketId(ticket);
    reservation.setStatus(ReservationStatusEnum.PENDING);
    reservationRepository.save(reservation);
    try {
      ticket.decreaseRemaining();
      ticketRepository.save(ticket);
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
    return true;
  }
}
