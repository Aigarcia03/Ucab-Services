package com.ucab.services.models;

import java.io.Serializable;
import java.util.Objects;

public class ServicioId implements Serializable {
    private String nombreCategoria;
    private Integer idPrestadora;
    private String descripcion;

    public ServicioId() {}

    public ServicioId(String nombreCategoria, Integer idPrestadora, String descripcion) {
        this.nombreCategoria = nombreCategoria;
        this.idPrestadora = idPrestadora;
        this.descripcion = descripcion;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public Integer getIdPrestadora() {
        return idPrestadora;
    }

    public void setIdPrestadora(Integer idPrestadora) {
        this.idPrestadora = idPrestadora;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServicioId that = (ServicioId) o;
        return Objects.equals(nombreCategoria, that.nombreCategoria) &&
               Objects.equals(idPrestadora, that.idPrestadora) &&
               Objects.equals(descripcion, that.descripcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombreCategoria, idPrestadora, descripcion);
    }
}
