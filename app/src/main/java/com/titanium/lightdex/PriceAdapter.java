package com.titanium.lightdex;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.titanium.lightdex.models.PrecioHora;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para mostrar la lista de precios por hora en un RecyclerView
 */
public class PriceAdapter extends RecyclerView.Adapter<PriceAdapter.PriceViewHolder> {
    
    private List<PrecioHora> precios;
    
    public PriceAdapter() {
        this.precios = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public PriceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_precio, parent, false);
        return new PriceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PriceViewHolder holder, int position) {
        PrecioHora precio = precios.get(position);
        holder.bind(precio);
    }
    
    @Override
    public int getItemCount() {
        return precios.size();
    }
    
    /**
     * Actualiza la lista de precios y notifica al RecyclerView
     * @param nuevosPrecios Nueva lista de precios
     */
    public void actualizarPrecios(List<PrecioHora> nuevosPrecios) {
        this.precios.clear();
        this.precios.addAll(nuevosPrecios);
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder que mantiene las referencias a las vistas de cada item
     */
    static class PriceViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvHora;
        private TextView tvPrecio;
        private TextView tvIndicador;
        private View viewColorBar;
        
        public PriceViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Inicializar las vistas (asegúrate de que estos IDs coincidan con item_precio.xml)
            tvHora = itemView.findViewById(R.id.tv_hora);
            tvPrecio = itemView.findViewById(R.id.tv_precio);
            tvIndicador = itemView.findViewById(R.id.tv_indicador);
            viewColorBar = itemView.findViewById(R.id.view_color_bar);
        }
        
        /**
         * Vincula los datos del PrecioHora con las vistas
         * @param precio Objeto con los datos a mostrar
         */
        public void bind(PrecioHora precio) {
            tvHora.setText(precio.getHora());
            tvPrecio.setText(precio.getPrecioFormateado() + " €/kWh");
            
            // Mostrar indicador si es el precio más alto o más bajo
            if (precio.esMasCaro()) {
                tvIndicador.setText("🔴 MÁS CARO");
                tvIndicador.setVisibility(View.VISIBLE);
                tvIndicador.setTextColor(Color.parseColor("#D32F2F"));
            } else if (precio.esMasBarato()) {
                tvIndicador.setText("🟢 MÁS BARATO");
                tvIndicador.setVisibility(View.VISIBLE);
                tvIndicador.setTextColor(Color.parseColor("#388E3C"));
            } else {
                tvIndicador.setVisibility(View.GONE);
            }
            
            // Cambiar color de la barra lateral según el nivel de precio
            int colorBarra = obtenerColorPorNivel(precio.getNivelPrecio());
            viewColorBar.setBackgroundColor(colorBarra);
            
            // Cambiar el color de fondo según el nivel
            int colorFondo = obtenerColorFondoPorNivel(precio.getNivelPrecio());
            itemView.setBackgroundColor(colorFondo);
        }
        
        /**
         * Obtiene el color para la barra lateral según el nivel de precio
         * @param nivel "bajo", "medio", o "alto"
         * @return Color en formato int
         */
        private int obtenerColorPorNivel(String nivel) {
            switch (nivel) {
                case "bajo":
                    return Color.parseColor("#4CAF50"); // Verde
                case "medio":
                    return Color.parseColor("#FFC107"); // Amarillo
                case "alto":
                    return Color.parseColor("#F44336"); // Rojo
                default:
                    return Color.GRAY;
            }
        }
        
        /**
         * Obtiene un color de fondo suave según el nivel de precio
         * @param nivel "bajo", "medio", o "alto"
         * @return Color en formato int
         */
        private int obtenerColorFondoPorNivel(String nivel) {
            switch (nivel) {
                case "bajo":
                    return Color.parseColor("#E8F5E9"); // Verde muy claro
                case "medio":
                    return Color.parseColor("#FFF9C4"); // Amarillo muy claro
                case "alto":
                    return Color.parseColor("#FFEBEE"); // Rojo muy claro
                default:
                    return Color.WHITE;
            }
        }
    }
}
