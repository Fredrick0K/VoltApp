package com.titanium.lightdex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
            tvPrecio.setText(precio.getPrecioFormateado());
            
            // Mostrar indicador si es el precio más alto o más bajo
            if (precio.esMasCaro()) {
                tvIndicador.setText(itemView.getContext().getString(R.string.maximo));
                tvIndicador.setVisibility(View.VISIBLE);
                tvIndicador.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.price_high));
            } else if (precio.esMasBarato()) {
                tvIndicador.setText(itemView.getContext().getString(R.string.minimo));
                tvIndicador.setVisibility(View.VISIBLE);
                tvIndicador.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.price_low));
            } else {
                tvIndicador.setVisibility(View.GONE);
            }
            
            // Width based on price relative to some max (hypothetically)
            // For now let's just use the level to color it
            int colorBarra = obtenerColorPorNivel(precio.getNivelPrecio());
            viewColorBar.setBackgroundColor(colorBarra);
            
            // In minimalist theme, we keep background consistent
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.bg_main));
        }
        
        private int obtenerColorPorNivel(String nivel) {
            switch (nivel) {
                case "bajo":
                    return ContextCompat.getColor(itemView.getContext(), R.color.price_low);
                case "medio":
                    return ContextCompat.getColor(itemView.getContext(), R.color.price_mid);
                case "alto":
                    return ContextCompat.getColor(itemView.getContext(), R.color.price_high);
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.text_muted);
            }
        }
    }
}
