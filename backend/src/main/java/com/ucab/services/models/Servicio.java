package com.ucab.services.models;

import jakarta.persistence.*;

@Entity
@Table(name = "servicio")
@IdClass(ServicioId.class)
public class Servicio {

    @Id
    @Column(name = "nombrecategoria")
    private String nombreCategoria;

    @Id
    @Column(name = "idprestadora")
    private Integer idPrestadora;

    @Id
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "preciobase")
    private Float precioBase;

    @Column(name = "ajuste")
    private Float ajuste;

    @Column(name = "ubicacion")
    private String ubicacion;

    public Servicio() {}

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

    public Float getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(Float precioBase) {
        this.precioBase = precioBase;
    }

    public Float getAjuste() {
        return ajuste;
    }

    public void setAjuste(Float ajuste) {
        this.ajuste = ajuste;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
