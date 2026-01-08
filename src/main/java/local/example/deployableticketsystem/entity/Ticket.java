package local.example.deployableticketsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "event_id")
  @NotNull
  private Event eventId;

  @NotBlank(message = "grade is mandatory")
  private String grade;

  @Min(0)
  @Max(1_000_000_000)
  @NotNull
  private Long price; // KRW

  @Min(1)
  @Max(1_000_000)
  @NotNull
  private Long totalQuantity;

  @Min(0)
  @Max(1_000_000)
  @NotNull
  private Long remainingQuantity;

  @CreationTimestamp
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  public Ticket(Event eventId, String grade, Long price, Long totalQuantity) {
    this.eventId = eventId;
    this.grade = grade;
    this.price = price;
    this.totalQuantity = totalQuantity;
    this.remainingQuantity = totalQuantity;
  }
}
