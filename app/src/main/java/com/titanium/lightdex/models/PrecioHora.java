package com.titanium.lightdex.models;

/**
 * Modelo que representa el precio de la electricidad en una hora específica
 */
public class PrecioHora {
    
    private String hora;          // Hora en formato "HH:mm"
    private double precio;        // Precio en €/MWh
    private String fecha;         // Fecha en formato "yyyy-MM-dd"
    private boolean esMasCaro;    // true si es la hora más cara del día
    private boolean esMasBarato;  // true si es la hora más barata del día
    
    // Constructor vacío
    public PrecioHora() {
    }
    
    // Constructor completo
    public PrecioHora(String hora, double precio, String fecha) {
        this.hora = hora;
        this.precio = precio;
        this.fecha = fecha;
        this.esMasCaro = false;
        this.esMasBarato = false;
    }
    
    // Getters y Setters
    public String getHora() {
        return hora;
    }
    
    public void setHora(String hora) {
        this.hora = hora;
    }
    
    public double getPrecio() {
        return precio;
    }
    
    public void setPrecio(double precio) {
        this.precio = precio;
    }
    
    public String getFecha() {
        return fecha;
    }
    
    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
    
    public boolean esMasCaro() {
        return esMasCaro;
    }
    
    public void setMasCaro(boolean masCaro) {
        this.esMasCaro = masCaro;
    }
    
    public boolean esMasBarato() {
        return esMasBarato;
    }
    
    public void setMasBarato(boolean masBarato) {
        this.esMasBarato = masBarato;
    }
    
    /**
     * Obtiene el precio formateado con 2 decimales
     * @return String con el precio formateado (ej: "125.50")
     */
    public String getPrecioFormateado() {
        return String.format("%.4f", precio / 1000).replace(".", ",");
    }
    
    public double getPrecioKwh() {
        return precio / 1000;
    }
    
    /**
     * Determina el nivel de precio (bajo, medio, alto)
     * basándose en rangos predefinidos
     * @return String: "bajo", "medio", o "alto"
     */
    public String getNivelPrecio() {
        if (precio < 100) {
            return "bajo";
        } else if (precio < 150) {
            return "medio";
        } else {
            return "alto";
        }
    }
    
    @Override
    public String toString() {
        return "PrecioHora{" +
                "hora='" + hora + '\'' +
                ", precio=" + precio +
                ", fecha='" + fecha + '\'' +
                '}';
    }
}
