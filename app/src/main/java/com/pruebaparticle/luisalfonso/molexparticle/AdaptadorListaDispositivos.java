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
    }

    public void sombrearTodos(boolean sombrear){
        for (int index = 0; index < dispositivos_seleccionados.size(); index++)
            dispositivos_seleccionados.set(index, sombrear);
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

    public void setAvatarDispositivo(Bitmap avatar, int index){
        avatares_dispositivos[index] = avatar;
    }

    public Bitmap getAvatarDispositivo(int index){
        return avatares_dispositivos[index];
    }

    @Override
    public int getCount() {
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
        View lv = convertView;
        if(convertView == null) lv = inflater.inflate(R.layout.device_list_item, null);
        TextView name = (TextView)lv.findViewById(R.id.tvDeviceName);
        TextView tv_conexiones = (TextView) lv.findViewById(R.id.tvOnline);
        ImageView avatar = (ImageView)lv.findViewById(R.id.ivAvatar);

        name.setText(nombres_dispositivos.get(position));
        if (dispositivos_conectados.get(position))
            tv_conexiones.setTextColor(ContextCompat.getColor(activity, R.color.verde_online));
        else
            tv_conexiones.setTextColor(ContextCompat.getColor(activity, R.color.rojo_offline));
        tv_conexiones.setText(dispositivos_conectados.get(position) ? activity.getString(R.string.online) :
                activity.getString(R.string.offline));
        if(avatares_dispositivos[position] != null)
            avatar.setImageBitmap(avatares_dispositivos[position]);

        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {               //Si se pulsa un avatar, se lanza el dialogo para cambiarlo
                activity.preguntarUsuario(position);
            }
        });

        if (dispositivos_seleccionados.get(position))
            lv.setBackgroundColor(ContextCompat.getColor(activity, R.color.gris_seleccion));
        else lv.setBackgroundColor(ContextCompat.getColor(activity, R.color.blanco));

        return lv;
    }

}