package com.ucab.services.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class TramiteId implements Serializable {
    private Integer ci;
    private Integer idPrestadora;
    private String nombreCategoria;
    private String descripcion;
    private LocalDateTime fechaCreacion;

    public TramiteId() {}

    public TramiteId(Integer ci, Integer idPrestadora, String nombreCategoria, String descripcion, LocalDateTime fechaCreacion) {
        this.ci = ci;
        this.idPrestadora = idPrestadora;
        this.nombreCategoria = nombreCategoria;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramiteId that = (TramiteId) o;
        return Objects.equals(ci, that.ci) && Objects.equals(idPrestadora, that.idPrestadora)
                && Objects.equals(nombreCategoria, that.nombreCategoria)
                && Objects.equals(descripcion, that.descripcion)
                && Objects.equals(fechaCreacion, that.fechaCreacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);
    }
}
