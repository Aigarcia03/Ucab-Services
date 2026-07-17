package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cursos")
@CrossOrigin(origins = "*")
public class CursosController {

    private final JdbcTemplate jdbcTemplate;

    public CursosController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getCursos(@RequestParam Integer ci, @RequestParam String rol) {
        try {
            if ("profesor".equals(rol)) {
                String sql = "SELECT DISTINCT c.Materia, c.FechaInicio, c.FechaFin " +
                             "FROM Curso c " +
                             "JOIN Asiste a ON c.Materia = a.Materia AND c.FechaInicio = a.FechaInicio " +
                             "WHERE a.CIProfesor = ? " +
                             "ORDER BY c.FechaInicio DESC";
                List<Map<String, Object>> cursos = jdbcTemplate.queryForList(sql, ci);
                return ResponseEntity.ok(cursos);
            } else if ("estudiante".equals(rol)) {
                String sql = "SELECT c.Materia, c.FechaInicio, c.FechaFin, a.Nota " +
                             "FROM Curso c " +
                             "JOIN Asiste a ON c.Materia = a.Materia AND c.FechaInicio = a.FechaInicio " +
                             "WHERE a.CIEstudiante = ? " +
                             "ORDER BY c.FechaInicio DESC";
                List<Map<String, Object>> cursos = jdbcTemplate.queryForList(sql, ci);
                return ResponseEntity.ok(cursos);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido. Use 'profesor' o 'estudiante'."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al consultar cursos: " + e.getMessage()));
        }
    }

    @GetMapping("/{materia}/{fechaInicio}/estudiantes")
    public ResponseEntity<?> getEstudiantesDelCurso(
            @PathVariable String materia,
            @PathVariable String fechaInicio,
            @RequestParam Integer ciProfesor) {
        try {
            String sql = "SELECT m.CI, m.PrimerNombre AS \"primerNombre\", m.PrimerApellido AS \"primerApellido\", a.Nota " +
                         "FROM Asiste a " +
                         "JOIN Miembro m ON a.CIEstudiante = m.CI " +
                         "WHERE a.Materia = ? AND a.FechaInicio = CAST(? AS DATE) AND a.CIProfesor = ? " +
                         "ORDER BY m.PrimerApellido, m.PrimerNombre";
            List<Map<String, Object>> estudiantes = jdbcTemplate.queryForList(sql, materia, fechaInicio, ciProfesor);
            return ResponseEntity.ok(estudiantes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al consultar estudiantes: " + e.getMessage()));
        }
    }

    @PutMapping("/{materia}/{fechaInicio}/nota")
    public ResponseEntity<?> actualizarNota(
            @PathVariable String materia,
            @PathVariable String fechaInicio,
            @RequestBody Map<String, Object> body) {
        try {
            Integer ciEstudiante = Integer.parseInt(body.get("ciEstudiante").toString());
            Double nota = Double.parseDouble(body.get("nota").toString());

            if (nota < 0 || nota > 20) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nota debe estar entre 0 y 20."));
            }

            Integer ciProfesor = Integer.parseInt(body.get("ciProfesor").toString());

            String checkSql = "SELECT FechaFin FROM Curso WHERE Materia = ? AND FechaInicio = CAST(? AS DATE)";
            List<Map<String, Object>> curso = jdbcTemplate.queryForList(checkSql, materia, fechaInicio);
            if (curso.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Curso no encontrado."));
            }
            Object fechaFinObj = curso.get(0).get("FechaFin");
            if (fechaFinObj != null) {
                java.sql.Date fechaFin = (java.sql.Date) fechaFinObj;
                if (fechaFin.toLocalDate().isBefore(java.time.LocalDate.now())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El período del curso ya finalizó. No se pueden modificar notas."));
                }
            }

            String sql = "UPDATE Asiste SET Nota = ? " +
                         "WHERE CIEstudiante = ? AND CIProfesor = ? AND Materia = ? AND FechaInicio = CAST(? AS DATE)";
            int updated = jdbcTemplate.update(sql, nota, ciEstudiante, ciProfesor, materia, fechaInicio);

            if (updated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontró la inscripción del estudiante en este curso."));
            }

            return ResponseEntity.ok(Map.of("mensaje", "Nota actualizada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al actualizar nota: " + e.getMessage()));
        }
    }
}
