package local.example.deployableticketsystem.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import local.example.deployableticketsystem.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t from Ticket t where t.id=:id")
  Optional<Ticket> findByIdWithLock(UUID id);

}
