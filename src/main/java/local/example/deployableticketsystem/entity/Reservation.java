package local.example.deployableticketsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private String userId;

  @ManyToOne
  @JoinColumn(name = "ticket_id")
  @NotNull
  private Ticket ticketId;

  @NotNull
  private ReservationStatusEnum status;

  @CreationTimestamp
  private Instant reservedAt;
}
