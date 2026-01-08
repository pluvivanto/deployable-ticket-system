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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

  public Reservation(String userId, Ticket ticketId) {
    this.userId = userId;
    this.ticketId = ticketId;
    this.status = ReservationStatusEnum.PENDING;
  }
}
