package com.pruebaparticle.luisalfonso.molexparticle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Clase AdaptadorListaModulos: Se encarga de generar la lista de modulos para el dispositivo pulsado en la activity
 * DispositivoSeleccionadoActivity
 * Created by LUIS ALFONSO on 22/12/2015.
 */
public class AdaptadorListaModulos extends BaseAdapter {

    private Activity activity;
    private String nombre_modulos[];
    private Bitmap imagen_modulos[];
    private String id_dispositivo;
    private static LayoutInflater inflater = null;

    public AdaptadorListaModulos(Activity activity, String[] nombre_modulos, String id_dispositivo, Bitmap[] imagen_modulos){
        this.activity = activity;
        this.nombre_modulos = nombre_modulos;
        this.imagen_modulos = imagen_modulos;
        this.id_dispositivo = id_dispositivo;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setNombresModulos(String nombre[]){
        System.arraycopy(nombre, 0, nombre_modulos, 0, Util.NUMERO_MODULOS);
    }

    public void setImagenesModulos(Bitmap imagenes[]){
        System.arraycopy(imagenes, 0, imagen_modulos, 0, Util.NUMERO_MODULOS);
    }

    @Override
    public int getCount() {
        return Util.NUMERO_MODULOS;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View lv = convertView;
        if(convertView == null) lv = inflater.inflate(R.layout.modulo_list_item, null);
        ImageView imagen_modulo = (ImageView)lv.findViewById(R.id.ivImagenModulo);
        TextView nombre_modulo = (TextView)lv.findViewById(R.id.tvNombreModulo);
        final ImageButton boton_encender_apagar_modulo = (ImageButton)lv.findViewById(R.id.btEncenderApagarModulo);

        boton_encender_apagar_modulo.setOnTouchListener(new View.OnTouchListener() {        //Al pulsar alguno de los botones encender
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Util.vibrar(activity);
                    ((ImageButton)v).setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.encender_btn_presionado));
                }
                if (event.getAction() == MotionEvent.ACTION_UP ||(event.getX() < 0 || event.getY() < 0 ||
                        event.getX() > boton_encender_apagar_modulo.getWidth() || event.getY() > boton_encender_apagar_modulo.getHeight())){
                    ((ImageButton)v).setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.encender_btn));
                }
                return true;
            }
        });

        imagen_modulo.setImageBitmap(imagen_modulos[position]);
        nombre_modulo.setText(nombre_modulos[position]);
        Util.ParticleAPI.leerVariableEstadoModulo(this, position, id_dispositivo, nombre_modulo);

        return lv;
    }

    public void notificarEstadoEncendido(TextView nombre_modulo) {
        nombre_modulo.setTextColor(ContextCompat.getColor(activity, R.color.verde_online));
    }

    public void notificarEstadoApagado(TextView nombre_modulo) {
        nombre_modulo.setTextColor(ContextCompat.getColor(activity, R.color.rojo_offline));
    }

    public void notificarEstadoError(TextView nombre_modulo) {
        nombre_modulo.setTextColor(ContextCompat.getColor(activity, R.color.gris_seleccion));
    }
}
