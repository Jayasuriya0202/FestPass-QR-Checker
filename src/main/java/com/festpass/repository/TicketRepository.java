package com.festpass.repository;

import com.festpass.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByQrCode(String qrCode);

    long countByEventId(Long eventId);

    long countByEventIdAndCheckedInTrue(Long eventId);

    boolean existsByEventId(Long eventId);

    List<Ticket> findByEventId(Long eventId);

    List<Ticket> findByAttendeeEmail(String email);
}
