package com.ucab.services.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tramite")
@IdClass(TramiteId.class)
public class Tramite {

    @Id
    @Column(name = "ci")
    private Integer ci;

    @Id
    @Column(name = "idprestadora")
    private Integer idPrestadora;

    @Id
    @Column(name = "nombrecategoria")
    private String nombreCategoria;

    @Id
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Id
    @Column(name = "fechacreacion", columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @Column(name = "fechacierre")
    private LocalDate fechaCierre;

    @Column(name = "estado")
    private String estado;

    public Tramite() {}

    public Integer getCi() { return ci; }
    public void setCi(Integer ci) { this.ci = ci; }
    public Integer getIdPrestadora() { return idPrestadora; }
    public void setIdPrestadora(Integer idPrestadora) { this.idPrestadora = idPrestadora; }
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
