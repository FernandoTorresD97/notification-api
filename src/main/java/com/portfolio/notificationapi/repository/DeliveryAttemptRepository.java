package com.portfolio.notificationapi.repository;
import com.portfolio.notificationapi.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt,Long> {
    List<DeliveryAttempt> findByNotification_IdOrderByAttemptNumberAsc(Long id);
    void deleteByNotification_Id(Long id);
}
