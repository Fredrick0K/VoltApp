package com.titanium.lightdex;

import android.content.Context;
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

public class HoraAdapter extends RecyclerView.Adapter<HoraAdapter.HoraViewHolder> {
    
    private List<PrecioHora> precios;
    private double precioMinimo;
    private double precioMaximo;
    
    public HoraAdapter() {
        this.precios = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public HoraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hora, parent, false);
        
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                (int) (42 * parent.getContext().getResources().getDisplayMetrics().density),
                (int) (85 * parent.getContext().getResources().getDisplayMetrics().density));
        params.setMarginEnd(2);
        view.setLayoutParams(params);
        
        return new HoraViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HoraViewHolder holder, int position) {
        PrecioHora precio = precios.get(position);
        holder.bind(precio, precioMinimo, precioMaximo);
    }
    
    @Override
    public int getItemCount() {
        return precios.size();
    }
    
    public void actualizarPrecios(List<PrecioHora> nuevosPrecios) {
        this.precios.clear();
        this.precios.addAll(nuevosPrecios);
        
        if (!nuevosPrecios.isEmpty()) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (PrecioHora p : nuevosPrecios) {
                double precioKwh = p.getPrecioKwh();
                if (precioKwh < min) min = precioKwh;
                if (precioKwh > max) max = precioKwh;
            }
            this.precioMinimo = min;
            this.precioMaximo = max;
        }
        
        notifyDataSetChanged();
    }
    
    static class HoraViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvHora;
        private TextView tvPrecio;
        
        public HoraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHora = itemView.findViewById(R.id.tv_hora);
            tvPrecio = itemView.findViewById(R.id.tv_precio);
        }
        
        public void bind(PrecioHora precio, double min, double max) {
            String hora = precio.getHora();
            tvHora.setText(hora.split(":")[0]);
            tvPrecio.setText(precio.getPrecioFormateado());
            
            int color = obtenerColorPorPrecio(precio.getPrecioKwh(), min, max);
            itemView.setBackgroundColor(color);
        }
        
        private int obtenerColorPorPrecio(double precio, double min, double max) {
            if (min >= max) {
                return Color.parseColor("#4CAF50");
            }
            
            double rango = max - min;
            double tercio = rango / 3;
            
            if (precio <= min + tercio) {
                return Color.parseColor("#2E7D32"); // Verde oscuro más intenso
            } else if (precio <= min + 2 * tercio) {
                return Color.parseColor("#F57F17"); // Amarillo/Naranja más intenso
            } else {
                return Color.parseColor("#C62828"); // Rojo más intenso
            }
        }
    }
}
