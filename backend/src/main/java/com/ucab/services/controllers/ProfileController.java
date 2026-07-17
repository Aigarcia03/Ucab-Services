package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/profile")
public class ProfileController {

    private final JdbcTemplate jdbcTemplate;

    public ProfileController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam(required = false) String email,
                                                          @RequestParam(required = false) Integer ci) {
        if ((email == null || email.isBlank()) && ci == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debes enviar el correo institucional o la CI del miembro."));
        }

        String sql = "SELECT m.CI, m.PrimerNombre, m.SegundoNombre, m.PrimerApellido, m.SegundoApellido, m.Sexo, " +
                     "m.CorreoInstitucional, m.DireccionHabitacion, m.FechaNacimiento, m.Telefono, m.Categoria, " +
                     "m.EstadoCuenta, m.UltimaConexion, m.FechaCambioContraseña, m.Foto, " +
                     "r.FechaInicio AS rolFechaInicio, r.FechaFin AS rolFechaFin, " +
                     "est.Semestre, est.Escuela, est.UnidadesCreditoAprobadas, est.PromedioPonderado, est.FacultadAdscripcion, " +
                     "b.TipoBeca, b.EstatusBeca, b.EstatusBeneficio, b.CumplimientoAcademico, " +
                     "pre.Asignatura, pre.HorasAyudantia, " +
                     "e.CargaHorariaSemanal, " +
                     "pr.EscalafonDocente, pr.CodigoInvestigador, " +
                     "pa.UnidadAdscripcionPresupuestaria, pa.CargoAdministrativo, " +
                     "eg.Titulo, eg.AñoGraduacion, eg.IndiceAcademicoFinal, " +
                     "CASE " +
                     "  WHEN b.CI IS NOT NULL THEN 'Estudiante (Becario)' " +
                     "  WHEN pre.CI IS NOT NULL THEN 'Estudiante (Preparador)' " +
                     "  WHEN est.CI IS NOT NULL THEN 'Estudiante' " +
                     "  WHEN pr.CI IS NOT NULL THEN 'Profesor' " +
                     "  WHEN pa.CI IS NOT NULL THEN 'Personal Administrativo' " +
                     "  WHEN e.CI IS NOT NULL THEN 'Empleado' " +
                     "  ELSE 'Miembro' " +
                     "END AS activeRole " +
                     "FROM Miembro m " +
                     "LEFT JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
                     "LEFT JOIN Estudiante est ON est.CI = r.CI AND est.FechaInicio = r.FechaInicio " +
                     "LEFT JOIN Becario b ON b.CI = est.CI AND b.FechaInicio = est.FechaInicio " +
                     "LEFT JOIN Preparador pre ON pre.CI = est.CI AND pre.FechaInicio = est.FechaInicio " +
                     "LEFT JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
                     "LEFT JOIN Profesor pr ON pr.CI = e.CI AND pr.FechaInicio = e.FechaInicio " +
                     "LEFT JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio " +
                     "LEFT JOIN Egresado eg ON eg.CI = m.CI " +
                     "WHERE m.CorreoInstitucional = ? OR m.CI = ?";
        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                ps -> {
                    ps.setString(1, email != null ? email : "");
                    if (ci != null) {
                        ps.setInt(2, ci);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("ci", rs.getInt("CI"));
                    profile.put("firstName", rs.getString("PrimerNombre"));
                    profile.put("secondName", rs.getString("SegundoNombre"));
                    profile.put("lastName", rs.getString("PrimerApellido"));
                    profile.put("secondLastName", rs.getString("SegundoApellido"));
                    profile.put("sex", rs.getString("Sexo"));
                    profile.put("email", rs.getString("CorreoInstitucional"));
                    profile.put("address", rs.getString("DireccionHabitacion"));
                    Date birthDate = rs.getDate("FechaNacimiento");
                    profile.put("birthDate", birthDate != null ? birthDate.toLocalDate().toString() : null);
                    profile.put("phone", rs.getString("Telefono"));
                    profile.put("category", rs.getString("Categoria"));
                    profile.put("accountStatus", rs.getString("EstadoCuenta"));
                    profile.put("activeRole", rs.getString("activeRole"));
                    profile.put("rolFechaInicio", rs.getDate("rolFechaInicio") != null ? rs.getDate("rolFechaInicio").toString() : null);
                    profile.put("rolFechaFin", rs.getDate("rolFechaFin") != null ? rs.getDate("rolFechaFin").toString() : null);
                    profile.put("semestre", rs.getObject("Semestre"));
                    profile.put("escuela", rs.getString("Escuela"));
                    profile.put("unidadesCredito", rs.getObject("UnidadesCreditoAprobadas"));
                    profile.put("promedioPonderado", rs.getObject("PromedioPonderado"));
                    profile.put("facultad", rs.getString("FacultadAdscripcion"));
                    profile.put("tipoBeca", rs.getString("TipoBeca"));
                    profile.put("estatusBeca", rs.getString("EstatusBeca"));
                    profile.put("estatusBeneficio", rs.getObject("EstatusBeneficio"));
                    profile.put("cumplimientoAcademico", rs.getObject("CumplimientoAcademico"));
                    profile.put("asignatura", rs.getString("Asignatura"));
                    profile.put("horasAyudantia", rs.getObject("HorasAyudantia"));
                    profile.put("cargaHoraria", rs.getString("CargaHorariaSemanal"));
                    profile.put("escalafon", rs.getString("EscalafonDocente"));
                    profile.put("codigoInvestigador", rs.getString("CodigoInvestigador"));
                    profile.put("unidadAdscripcion", rs.getString("UnidadAdscripcionPresupuestaria"));
                    profile.put("cargoAdministrativo", rs.getString("CargoAdministrativo"));
                    profile.put("titulo", rs.getString("Titulo"));
                    profile.put("anioGraduacion", rs.getDate("AñoGraduacion") != null ? rs.getDate("AñoGraduacion").toString() : null);
                    profile.put("indiceAcademico", rs.getObject("IndiceAcademicoFinal"));
                    profile.put("lastConnection", rs.getString("UltimaConexion"));
                    profile.put("passwordChangeDate", rs.getTimestamp("FechaCambioContraseña") != null
                            ? rs.getTimestamp("FechaCambioContraseña").toString() : null);
                    byte[] avatarBytes = rs.getBytes("Foto");
                    profile.put("avatar", avatarBytes != null ? toImageDataUrl(avatarBytes) : null);
                    return profile;
                });

        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "No se encontró el perfil del miembro."));
        }

        return ResponseEntity.ok(results.get(0));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> updateProfileAvatar(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            String avatarData = payload.get("avatar") == null ? null : String.valueOf(payload.get("avatar")).trim();

            if (ciValue == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debes enviar la CI del miembro."));
            }

            if (avatarData == null || avatarData.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debes seleccionar una imagen válida."));
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));
            byte[] avatarBytes = decodeAvatarPayload(avatarData);
            String sql = "UPDATE Miembro SET Foto = ? WHERE CI = ?";
            int updatedRows = jdbcTemplate.update(sql, avatarBytes, ci);

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontró el miembro para actualizar el avatar."));
            }

            return ResponseEntity.ok(Map.of(
                "message", "Avatar actualizado correctamente.",
                "ci", ci,
                "avatar", toImageDataUrl(avatarBytes)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen enviada no tiene un formato válido."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No se pudo actualizar el avatar: " + e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateProfileField(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            String field = String.valueOf(payload.get("field"));
            String value = payload.get("value") == null ? null : String.valueOf(payload.get("value")).trim();

            if (ciValue == null || field == null || field.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debes enviar la CI y el campo a modificar."));
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));

            if (value == null || value.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nuevo valor no puede estar vacío."));
            }

            String column;
            switch (field) {
                case "secondName" -> column = "SegundoNombre";
                case "secondLastName" -> column = "SegundoApellido";
                case "address" -> column = "DireccionHabitacion";
                case "phone" -> column = "Telefono";
                default -> {
                    return ResponseEntity.badRequest().body(Map.of("error", "Campo no permitido para modificación."));
                }
            }

            if ("SegundoNombre".equals(column) || "SegundoApellido".equals(column)) {
                String currentValue = jdbcTemplate.queryForObject(
                        "SELECT " + column + " FROM Miembro WHERE CI = ?",
                        new Object[]{ci},
                        String.class
                );
                if (currentValue != null && !currentValue.isBlank()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", column + " ya tiene valor y no se puede modificar."));
                }
            }

            String sql = "UPDATE Miembro SET " + column + " = ? WHERE CI = ?";
            int updatedRows = jdbcTemplate.update(sql, value, ci);

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontró el miembro para actualizar."));
            }

            return ResponseEntity.ok(Map.of(
                "message", "Campo actualizado correctamente.",
                "field", field,
                "value", value
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "CI inválida."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No se pudo actualizar el perfil: " + e.getMessage()));
        }
    }

    private String toImageDataUrl(byte[] avatarBytes) {
        String mimeType = detectImageMimeType(avatarBytes);
        String base64 = Base64.getEncoder().encodeToString(avatarBytes);
        return "data:" + mimeType + ";base64," + base64;
    }

    private byte[] decodeAvatarPayload(String avatarData) {
        String base64 = avatarData;
        int commaIndex = avatarData.indexOf(',');
        if (commaIndex >= 0) {
            base64 = avatarData.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(base64);
    }

    private String detectImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return "image/png";

        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
            return "image/png";
        }

        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }

        if ((bytes[0] & 0xFF) == 'G' && (bytes[1] & 0xFF) == 'I' && (bytes[2] & 0xFF) == 'F') {
            return "image/gif";
        }

        if (bytes.length >= 12 && (bytes[0] & 0xFF) == 'R' && (bytes[1] & 0xFF) == 'I'
                && (bytes[2] & 0xFF) == 'F' && (bytes[8] & 0xFF) == 'W'
                && (bytes[9] & 0xFF) == 'E' && (bytes[10] & 0xFF) == 'B'
                && (bytes[11] & 0xFF) == 'P') {
            return "image/webp";
        }

        return "image/png";
    }
}
