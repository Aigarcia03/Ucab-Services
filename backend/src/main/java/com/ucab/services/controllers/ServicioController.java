package com.ucab.services.controllers;

import com.ucab.services.models.Servicio;
import com.ucab.services.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class ServicioController {

    @Autowired
    private ServicioRepository servicioRepository;

    @GetMapping
    public List<Servicio> getServicios(@RequestParam(required = false) String categoria) {
        if (categoria != null && !categoria.isEmpty()) {
            return servicioRepository.findByNombreCategoria(categoria);
        }
        return servicioRepository.findAll();
    }
}
