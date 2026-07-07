package com.ucab.services.repositories;

import com.ucab.services.models.Tramite;
import com.ucab.services.models.TramiteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TramiteRepository extends JpaRepository<Tramite, TramiteId> {
    List<Tramite> findByCi(Integer ci);
}
