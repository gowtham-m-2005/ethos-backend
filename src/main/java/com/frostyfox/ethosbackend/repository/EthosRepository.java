package com.frostyfox.ethosbackend.repository;

import com.frostyfox.ethosbackend.model.EthosModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EthosRepository extends JpaRepository<EthosModel, Long> {
}
