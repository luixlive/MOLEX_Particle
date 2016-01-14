package com.pruebaparticle.luisalfonso.molexparticle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Clase AdaptadorListaModulos: Se encarga de generar la lista de modulos para el dispositivo pulsado en la activity
 * DispositivoSeleccionadoActivity
 * Created by LUIS ALFONSO on 22/12/2015.
 */
public class AdaptadorListaModulos extends BaseAdapter {

    private DispositivoSeleccionadoActivity activity;
    private String nombre_modulos[];
    private Bitmap imagen_modulos[];
    private String id_dispositivo;
    private boolean modulo_encendido[];
    private TextView tvNombre_modulos[];
    private View vista_modificandose;
    private boolean vista_modificandose_ahora = false;
    private static LayoutInflater inflater = null;

    public AdaptadorListaModulos(DispositivoSeleccionadoActivity activity, String[] nombre_modulos, String id_dispositivo,
                                 Bitmap[] imagen_modulos){
        this.activity = activity;
        this.nombre_modulos = nombre_modulos;
        this.imagen_modulos = imagen_modulos;
        this.id_dispositivo = id_dispositivo;

        tvNombre_modulos = new TextView[Util.NUMERO_MODULOS];
        modulo_encendido = new boolean[Util.NUMERO_MODULOS];
        for (int index = 0; index < Util.NUMERO_MODULOS; index++) modulo_encendido[index] = false;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        View lv = convertView;
        if(convertView == null) lv = inflater.inflate(R.layout.modulo_list_item, null);
        ImageView imagen_modulo = (ImageView)lv.findViewById(R.id.ivImagenModulo);
        tvNombre_modulos[position] = (TextView)lv.findViewById(R.id.tvNombreModulo);
        final ImageButton boton_encender_apagar_modulo = (ImageButton)lv.findViewById(R.id.btEncenderApagarModulo);

        final View finalLv = lv;
        tvNombre_modulos[position].setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (vista_modificandose_ahora)
                    cambioNombreListo();
                vista_modificandose_ahora = true;
                Util.vibrar(activity);
                String hint = ((TextView)v).getText().toString();
                v.setVisibility(View.GONE);
                EditText etNombre_modulo = (EditText) finalLv.findViewById(R.id.etNombreModulo);
                etNombre_modulo.setHint(hint);
                etNombre_modulo.setVisibility(View.VISIBLE);
                activity.menuCambioNombreModulo();
                vista_modificandose = finalLv;
                return true;
            }
        });

        imagen_modulo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Util.vibrar(activity);
                activity.cambioImagenModulo(position);
                return true;
            }
        });

        boton_encender_apagar_modulo.setOnTouchListener(new View.OnTouchListener() {        //Al pulsar alguno de los botones encender
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Util.vibrar(activity);
                    ((ImageButton)v).setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.encender_btn_presionado));
                    if (modulo_encendido[position]) Util.ParticleAPI.encenderModulo(AdaptadorListaModulos.this,
                            id_dispositivo, position);
                    else Util.ParticleAPI.apagarModulo(AdaptadorListaModulos.this, id_dispositivo, position);
                }
                if (event.getAction() == MotionEvent.ACTION_UP ||(event.getX() < 0 || event.getY() < 0 ||
                        event.getX() > boton_encender_apagar_modulo.getWidth() || event.getY() > boton_encender_apagar_modulo.getHeight())){
                    ((ImageButton)v).setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.encender_btn));
                }
                return true;
            }
        });

        imagen_modulo.setImageBitmap(imagen_modulos[position]);
        tvNombre_modulos[position].setText(nombre_modulos[position]);
        tvNombre_modulos[position].setTextColor(ContextCompat.getColor(activity, R.color.gris_seleccion));
        Util.ParticleAPI.leerVariableEstadoModulo(this, position, id_dispositivo);

        return lv;
    }

    public void notificarEstadoEncendido(int posicion) {
        tvNombre_modulos[posicion].setTextColor(ContextCompat.getColor(activity, R.color.verde_online));
        modulo_encendido[posicion] = true;
    }

    public void notificarEstadoApagado(int posicion) {
        tvNombre_modulos[posicion].setTextColor(ContextCompat.getColor(activity, R.color.rojo_offline));
        modulo_encendido[posicion] = false;
    }

    public void notificarEstadoError(int posicion) {
        tvNombre_modulos[posicion].setTextColor(ContextCompat.getColor(activity, R.color.gris_seleccion));
    }

    public void cambioNombreListo() {
        EditText etNombre_modulo = (EditText)vista_modificandose.findViewById(R.id.etNombreModulo);
        String nuevo_nombre = etNombre_modulo.getText().toString();
        if (nuevo_nombre.isEmpty()) {
            cambioNombreCancelado();
            return;
        }
        etNombre_modulo.setVisibility(View.GONE);
        etNombre_modulo.setText(null);
        TextView vista_modificada = (TextView)vista_modificandose.findViewById(R.id.tvNombreModulo);
        vista_modificada.setText(nuevo_nombre);
        vista_modificada.setVisibility(View.VISIBLE);
        vista_modificandose_ahora = false;
    }

    public void cambioNombreCancelado() {
        EditText etNombre_modulo = (EditText)vista_modificandose.findViewById(R.id.etNombreModulo);
        etNombre_modulo.setVisibility(View.GONE);
        etNombre_modulo.setText(null);
        (vista_modificandose.findViewById(R.id.tvNombreModulo)).setVisibility(View.VISIBLE);
        vista_modificandose_ahora = false;
    }

    public String[] getNombresModulos() {
        String[] nombres_modulos = new String[3];
        nombres_modulos[0] = tvNombre_modulos[0].getText().toString();
        nombres_modulos[1] = tvNombre_modulos[1].getText().toString();
        nombres_modulos[2] = tvNombre_modulos[2].getText().toString();
        return  nombres_modulos;
    }
}
