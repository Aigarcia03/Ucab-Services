package com.ucab.services.repositories;

import com.ucab.services.models.Servicio;
import com.ucab.services.models.ServicioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, ServicioId> {
    List<Servicio> findByNombreCategoria(String nombreCategoria);
}
