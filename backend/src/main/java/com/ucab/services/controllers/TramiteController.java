package com.ucab.services.controllers;

import com.ucab.services.models.Tramite;
import com.ucab.services.models.Servicio;
import com.ucab.services.repositories.TramiteRepository;
import com.ucab.services.repositories.ServicioRepository;
import com.ucab.services.models.ServicioId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tramites")
@CrossOrigin(origins = "*")
public class TramiteController {

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    /**
     * GET /api/tramites?ci=12345
     * Returns all tramites for a given member CI, enriched with the service's price.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTramitesByCi(@RequestParam Integer ci) {
        List<Tramite> tramites = tramiteRepository.findByCi(ci);

        List<Map<String, Object>> result = tramites.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("ci", t.getCi());
            item.put("nombreCategoria", t.getNombreCategoria());
            item.put("idPrestadora", t.getIdPrestadora());
            item.put("descripcion", t.getDescripcion());
            item.put("fechaCreacion", t.getFechaCreacion());
            item.put("estado", t.getEstado());

            // Enrich with price from Servicio table
            Optional<Servicio> servicio = servicioRepository.findById(
                new ServicioId(t.getNombreCategoria(), t.getIdPrestadora(), t.getDescripcion())
            );
            item.put("precioBase", servicio.map(Servicio::getPrecioBase).orElse(0f));

            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/tramites
     * Creates a new tramite (adds a service to Estado de Cuenta).
     * Body: { ci, idPrestadora, nombreCategoria, descripcion }
     */
    @PostMapping
    public ResponseEntity<?> crearTramite(@RequestBody Map<String, Object> body) {
        try {
            Tramite t = new Tramite();
            t.setCi(Integer.parseInt(body.get("ci").toString()));
            t.setIdPrestadora(Integer.parseInt(body.get("idPrestadora").toString()));
            t.setNombreCategoria(body.get("nombreCategoria").toString());
            t.setDescripcion(body.get("descripcion").toString());
            t.setFechaCreacion(LocalDate.now());
            t.setEstado("activo");

            tramiteRepository.save(t);
            return ResponseEntity.ok(Map.of("mensaje", "Trámite creado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al crear el trámite: " + e.getMessage()));
        }
    }
}
