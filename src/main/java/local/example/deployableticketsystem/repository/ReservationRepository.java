package local.example.deployableticketsystem.repository;

import java.util.UUID;
import local.example.deployableticketsystem.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

}
