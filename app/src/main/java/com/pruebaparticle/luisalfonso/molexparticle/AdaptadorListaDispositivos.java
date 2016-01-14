package com.pruebaparticle.luisalfonso.molexparticle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adaptador para la lista personalizada de nuestra app, donde se muestra un avatar, el nombre del dispositivo y su
 * estado de conexion
 * Created by LUIS ALFONSO on 18/12/2015.
 */
public class AdaptadorListaDispositivos extends BaseAdapter {

    private ArrayList<String> nombres_dispositivos = new ArrayList<>();
    private ArrayList<Boolean> dispositivos_conectados = new ArrayList<>();
    private ArrayList<Boolean> dispositivos_seleccionados = new ArrayList<>();
    private Bitmap[] avatares_dispositivos;
    private static LayoutInflater inflater = null;
    private DispositivosActivity activity;
    private boolean filtrar_dispositivos_conectados = false;

    public AdaptadorListaDispositivos(DispositivosActivity activity, ArrayList<String> nombres_dispositivos,
                                      ArrayList<Boolean> dispositivos_conectados, Bitmap[] avatares_dispositivos){
        this.activity = activity;
        this.nombres_dispositivos = nombres_dispositivos;
        this.dispositivos_conectados = dispositivos_conectados;
        this.avatares_dispositivos = avatares_dispositivos;
        for (Boolean ignored : dispositivos_conectados)
            dispositivos_seleccionados.add(false);
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void sombrear(boolean sombrear, int posicion){
        dispositivos_seleccionados.set(posicion, sombrear);
        notifyDataSetChanged();
    }

    public void sombrearTodos(boolean sombrear){
        for (int index = 0; index < dispositivos_seleccionados.size(); index++)
            dispositivos_seleccionados.set(index, sombrear);
        notifyDataSetChanged();
    }

    public void setNombresDispositivos(ArrayList<String> nombres){
        nombres_dispositivos.clear();
        for (int index = 0; index < nombres.size(); index++)
            nombres_dispositivos.add(nombres.get(index));
    }

    public void setConexionesDispositivos(ArrayList<Boolean> conexiones){
        dispositivos_conectados.clear();
        for (int index = 0; index < conexiones.size(); index++)
            dispositivos_conectados.add(conexiones.get(index));
    }

    @Override
    public int getCount() {
        if (filtrar_dispositivos_conectados){
            int count = 0;
            for (boolean conectado: dispositivos_conectados)
                if (conectado) count++;
            return count;
        }
        return nombres_dispositivos.size();
    }

    @Override
    public Object getItem(int position) {
        return nombres_dispositivos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View gv = convertView;
        if(convertView == null) gv = inflater.inflate(R.layout.device_list_item, null);
        TextView name = (TextView)gv.findViewById(R.id.tvDeviceName);
        TextView tv_conexiones = (TextView) gv.findViewById(R.id.tvOnline);
        ImageView avatar = (ImageView)gv.findViewById(R.id.ivAvatar);

        int posicion = position;
        if (filtrar_dispositivos_conectados){
            posicion = 0;
            int conteo_regresivo = position;
            for (boolean conectado: dispositivos_conectados){
                if (conectado)
                    conteo_regresivo--;
                if (conteo_regresivo == -1)
                    break;
                posicion++;
            }
        }

        name.setText(nombres_dispositivos.get(posicion));
        if (dispositivos_conectados.get(posicion))
            tv_conexiones.setTextColor(ContextCompat.getColor(activity, R.color.verde_online));
        else
            tv_conexiones.setTextColor(ContextCompat.getColor(activity, R.color.rojo_offline));
        tv_conexiones.setText(dispositivos_conectados.get(posicion) ? activity.getString(R.string.online) :
                activity.getString(R.string.offline));
        if(avatares_dispositivos[posicion] != null)
            avatar.setImageBitmap(avatares_dispositivos[posicion]);
        if (dispositivos_seleccionados.get(posicion))
            gv.setBackgroundColor(ContextCompat.getColor(activity, R.color.gris_seleccion));
        else gv.setBackgroundColor(ContextCompat.getColor(activity, R.color.blanco));
        return gv;
    }

    public void setAvatares(Bitmap[] avatares) {
        System.arraycopy(avatares, 0, avatares_dispositivos, 0, avatares_dispositivos.length);
    }

    public boolean filtrarDispositivosPresionado() {
        filtrar_dispositivos_conectados = !filtrar_dispositivos_conectados;
        this.notifyDataSetChanged();
        return filtrar_dispositivos_conectados;
    }
}