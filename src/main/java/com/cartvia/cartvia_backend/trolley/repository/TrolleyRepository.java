package com.cartvia.cartvia_backend.trolley.repository;

import com.cartvia.cartvia_backend.common.enums.TrolleyStatus;
import com.cartvia.cartvia_backend.trolley.entity.Trolley;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrolleyRepository extends JpaRepository<Trolley, UUID> {

    Optional<Trolley> findByTrolleyCode(String trolleyCode);

    boolean existsByTrolleyCode(String trolleyCode);

    List<Trolley> findByStore_Id(UUID storeId);

    Optional<Trolley> findByDeviceTokenHash(String deviceTokenHash);

    List<Trolley> findByStatusInAndLastSeenAtBefore(List<TrolleyStatus> statuses, LocalDateTime cutoff);

    List<Trolley> findAll();

}
