package com.titanium.lightdex;

import android.content.Context;

import com.titanium.lightdex.models.PrecioHora;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Servicio para obtener los precios de electricidad desde la API de REE
 * (Red Eléctrica de España)
 */
public class ElectricityApiService {
    
    private static final String TAG = "ElectricityAPI";
    private static final String BASE_URL = "https://apidatos.ree.es/es/datos/mercados/precios-mercados-tiempo-real";
    
    private Context context;
    
    public ElectricityApiService(Context context) {
        this.context = context;
    }
    
    /**
     * Obtiene los precios de electricidad para el día actual
     * Este método debe ejecutarse en un hilo secundario (no en el UI thread)
     * 
     * @return Lista de objetos PrecioHora con los precios del día
     * @throws Exception Si hay error de conexión o la API falla
     */
    public List<PrecioHora> obtenerPreciosHoy() throws Exception {
        List<PrecioHora> precios = new ArrayList<>();
        
        try {
            // Obtener fecha actual en formato yyyy-MM-dd
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String fechaHoy = dateFormat.format(new Date());
            
            // Construir la URL con parámetros
            String urlString = BASE_URL + 
                    "?start_date=" + fechaHoy + "T00:00" +
                    "&end_date=" + fechaHoy + "T23:59" +
                    "&time_trunc=hour";
            
            SecureLogger.d(TAG, "Consultando API");
            
            // Realizar petición HTTP
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 segundos timeout
            connection.setReadTimeout(10000);
            
            // Leer la respuesta
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Procesar el JSON
                precios = procesarRespuestaJSON(response.toString(), fechaHoy);
                
                SecureLogger.d(TAG, "Precios obtenidos: " + precios.size());
                
                if (precios.isEmpty()) {
                    throw new Exception("La API respondió pero no hay datos de precios");
                }
                
            } else {
                String errorMsg = "Error en la API. Código HTTP: " + responseCode;
                SecureLogger.error(TAG, errorMsg);
                throw new Exception(errorMsg);
            }
            
            connection.disconnect();
            
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("Tiempo de espera agotado. Verifica tu conexión a internet.");
        } catch (java.net.UnknownHostException e) {
            throw new Exception("No se puede conectar al servidor. Verifica tu conexión.");
        } catch (Exception e) {
            SecureLogger.error(TAG, "Error al obtener precios");
            throw new Exception("Error al obtener precios: " + e.getMessage());
        }
        
        return precios;
    }
    
    /**
     * Procesa la respuesta JSON de la API de REE
     * 
     * @param jsonString Respuesta JSON en formato String
     * @param fecha Fecha de los precios
     * @return Lista de PrecioHora parseados
     */
    private List<PrecioHora> procesarRespuestaJSON(String jsonString, String fecha) {
        List<PrecioHora> precios = new ArrayList<>();
        
        try {
            JSONObject jsonRoot = new JSONObject(jsonString);
            
            // Navegar por la estructura JSON de la API REE
            JSONObject included = jsonRoot.getJSONArray("included").getJSONObject(0);
            JSONObject attributes = included.getJSONObject("attributes");
            JSONArray values = attributes.getJSONArray("values");
            
            // Procesar cada valor (cada hora)
            for (int i = 0; i < values.length(); i++) {
                JSONObject valueObj = values.getJSONObject(i);
                
                double precioMWh = valueObj.getDouble("value");
                String datetime = valueObj.getString("datetime");
                
                // Extraer la hora del datetime (formato: 2024-02-16T14:00:00.000+01:00)
                String hora = extraerHora(datetime);
                
                PrecioHora precioHora = new PrecioHora(hora, precioMWh, fecha);
                precios.add(precioHora);
            }
            
            // Marcar las horas más caras y más baratas
            marcarExtremosPrecios(precios);
            
        } catch (JSONException e) {
            SecureLogger.error(TAG, "Error al parsear JSON");
        }
        
        return precios;
    }
    
    /**
     * Extrae la hora en formato HH:mm desde un datetime ISO
     * 
     * @param datetime String en formato ISO (ej: "2024-02-16T14:00:00.000+01:00")
     * @return Hora en formato "HH:mm" (ej: "14:00")
     */
    private String extraerHora(String datetime) {
        try {
            // Formato: 2024-02-16T14:00:00.000+01:00
            // Extraemos solo la parte de la hora
            String[] parts = datetime.split("T");
            if (parts.length > 1) {
                String timePart = parts[1].substring(0, 5); // "14:00"
                return timePart;
            }
        } catch (Exception e) {
            SecureLogger.error(TAG, "Error al extraer hora");
        }
        return "00:00";
    }
    
    /**
     * Marca las horas con precio más alto y más bajo
     * 
     * @param precios Lista de precios a analizar
     */
    private void marcarExtremosPrecios(List<PrecioHora> precios) {
        if (precios.isEmpty()) return;
        
        double precioMax = Double.MIN_VALUE;
        double precioMin = Double.MAX_VALUE;
        int indexMax = 0;
        int indexMin = 0;
        
        // Encontrar máximo y mínimo
        for (int i = 0; i < precios.size(); i++) {
            double precio = precios.get(i).getPrecio();
            
            if (precio > precioMax) {
                precioMax = precio;
                indexMax = i;
            }
            
            if (precio < precioMin) {
                precioMin = precio;
                indexMin = i;
            }
        }
        
        // Marcar los extremos
        precios.get(indexMax).setMasCaro(true);
        precios.get(indexMin).setMasBarato(true);
        
        SecureLogger.d(TAG, "Precio más alto y bajo calculados");
    }
    
    /**
     * Calcula el precio promedio del día
     * 
     * @param precios Lista de precios
     * @return Precio promedio en €/MWh
     */
    public double calcularPromedio(List<PrecioHora> precios) {
        if (precios.isEmpty()) return 0.0;
        
        double suma = 0.0;
        for (PrecioHora precio : precios) {
            suma += precio.getPrecio();
        }
        
        return suma / precios.size();
    }
    
    /**
     * Obtiene el precio más alto del día
     * 
     * @param precios Lista de precios
     * @return PrecioHora con el precio más alto, o null si la lista está vacía
     */
    public PrecioHora obtenerPrecioMasAlto(List<PrecioHora> precios) {
        if (precios.isEmpty()) return null;
        
        PrecioHora masAlto = precios.get(0);
        for (PrecioHora precio : precios) {
            if (precio.getPrecio() > masAlto.getPrecio()) {
                masAlto = precio;
            }
        }
        return masAlto;
    }
    
    /**
     * Obtiene el precio más bajo del día
     * 
     * @param precios Lista de precios
     * @return PrecioHora con el precio más bajo, o null si la lista está vacía
     */
    public PrecioHora obtenerPrecioMasBajo(List<PrecioHora> precios) {
        if (precios.isEmpty()) return null;
        
        PrecioHora masBajo = precios.get(0);
        for (PrecioHora precio : precios) {
            if (precio.getPrecio() < masBajo.getPrecio()) {
                masBajo = precio;
            }
        }
        return masBajo;
    }
}
