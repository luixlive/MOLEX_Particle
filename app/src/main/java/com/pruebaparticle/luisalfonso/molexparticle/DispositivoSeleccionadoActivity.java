package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Clase DispositivoSeleccionadoActivity: muestra informacion y acciones que se pueden realizar con el dispositivo seleccionado
 * Created by LUIS ALFONSO on 21/12/2015.
 */
public class DispositivoSeleccionadoActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_EDITAR_AVATAR = 2913;
    private final static int REQUEST_CODE_EDITAR_MODULOS = 2912;
    private final static int NUMERO_MODULOS = 3;
    private final static double RELACION_WIDTH_IMAGEN = 1 / 6.0;
    private final static double RELACION_WIDTH_AVATAR = 1 / 2.0;

    private String id_dispositivo;              //Informacion del dispositivo enviada por la activity DispositivosActivity
    private String nombre_dispositivo;
    private Boolean conexion_dispositivo;
    private String directorio_avatares;
    private boolean almacenamiento_avatares_posible;

    private boolean almacenamiento_modulos_posible;
    private  String directorio_modulos;
    private String nombre_modulos[] = new String[NUMERO_MODULOS];

    private static Integer index_dispositivo = null;
    private static TextView tv_conexion_dispositivo;
    private static TextView tv_nombre_dispositivo;
    private ListView lv_modulos;
    private ImageView iv_avatar;

    private int ancho_imagen;
    private int ancho_avatar;

    private static boolean avatar_cambiado = false;

    private AdaptadorListaModulos adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivo_seleccionado);

        capturarInformacionDispositivo();
        obtenerVistas();
        prepararImagenesModulos();
        actualizarVistas();
        crearListaModulos();
    }

    /**
     * cambiarAvatar: se llama al pulsar el avatar, lanza un dialogo que le permite al usuario elegir si desea cambiar su
     * avatar
     * @param imagen: vista de la imagen del avatar
     */
    public void cambiarAvatar(View imagen){
        Util.vibrar(this);
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);        //Creamos un dialogo con la pregunta
        dialogo.setTitle(R.string.dialogo_cambiar_imagen);
        dialogo.setCancelable(false);
        dialogo.setMessage(R.string.mensaje_dialogo_cambiar_imagen);
        dialogo.setPositiveButton(R.string.cambiar_imagen_si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_CODE_EDITAR_AVATAR);
            }
        });
        dialogo.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert_dialogo = dialogo.create();
        alert_dialogo.show();
    }

    /**
     * editarDispositivo: se llama cuando el usuario pulsa editar, invoca la activity EditarModulos y se le pasa la informacion
     * que ya se leyo del almacenamiento
     * @param boton: vista del boton editar
     */
    public void editarDispositivo(View boton){
        Util.vibrar(this);
        boton.setBackgroundColor(ContextCompat.getColor(this, R.color.blanco));
        ((Button)boton).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

        Intent intent = new Intent(this, EditarModulos.class);
        intent.putExtra("nombre_dispositivo", nombre_dispositivo);
        intent.putExtra("id_dispositivo", id_dispositivo);
        intent.putExtra("nombre_modulo_1", nombre_modulos[0]);
        intent.putExtra("nombre_modulo_2", nombre_modulos[1]);
        intent.putExtra("nombre_modulo_3", nombre_modulos[2]);
        intent.putExtra("directorio_modulos", directorio_modulos);
        intent.putExtra("almacenamiento_modulos_posible", almacenamiento_modulos_posible);
        intent.putExtra("configuracion_inicial", false);
        startActivityForResult(intent, REQUEST_CODE_EDITAR_MODULOS);

        boton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        ((Button) boton).setTextColor(ContextCompat.getColor(this, R.color.blanco));
    }

    /**
     * capturarInformacionDispositivo: recibe la informacion del intent de la activity pasada, que contiene la informacion
     * del dispositivo seleccionado
     */
    private void capturarInformacionDispositivo() {
        Intent intent = getIntent();
        index_dispositivo = intent.getIntExtra("index_dispositivo", -1);
        id_dispositivo = intent.getStringExtra("id_dispositivo");
        nombre_dispositivo = intent.getStringExtra("nombre_dispositivo");
        conexion_dispositivo = intent.getBooleanExtra("conexion_dispositivo", false);
        directorio_avatares = intent.getStringExtra("directorio_avatares");
        directorio_modulos = intent.getStringExtra("directorio_app") + File.separator + "Modulos" + File.separator + id_dispositivo;
        almacenamiento_avatares_posible = intent.getBooleanExtra("almacenamiento_avatares_posible", false);
        almacenamiento_modulos_posible = Util.comprobarDirectorio(this, new File(directorio_modulos));
    }

    /**
     * obtenerVistas: obtiene las vistas necesarias para las variables globales de esta clase
     */
    private void obtenerVistas() {
        tv_conexion_dispositivo = (TextView)findViewById(R.id.tvConexionDispositivoGrande);
        tv_nombre_dispositivo = (TextView)findViewById(R.id.tvNombreDispositivoGrande);
        lv_modulos = (ListView)findViewById(R.id.lvModulos);
        iv_avatar = (ImageView)findViewById(R.id.ivAvatarGrande);
    }

    /**
     * prepararImagenesModulos: obtiene las medidas de la pantalla del smartphone utilizado y calcula los pixeles a los que se deben
     * escalar las imagenes para dar una buena apariencia
     */
    private void prepararImagenesModulos() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ancho_avatar = (int)(metrics.widthPixels * RELACION_WIDTH_AVATAR);
        ancho_imagen = (int)(metrics.widthPixels * RELACION_WIDTH_IMAGEN);
    }

    /**
     * actualizarVistas: actualiza las vistas del layout de esta activity con la informacion obtenida
     */
    private void actualizarVistas() {
        tv_nombre_dispositivo.setText(nombre_dispositivo);
        if (conexion_dispositivo)
            tv_conexion_dispositivo.setTextColor(ContextCompat.getColor(this, R.color.verde_online));
        else
            tv_conexion_dispositivo.setTextColor(ContextCompat.getColor(this, R.color.rojo_offline));
        tv_conexion_dispositivo.setText(conexion_dispositivo ? getString(R.string.online) : getString(R.string.offline));
        extraerAvatar(directorio_avatares, id_dispositivo, almacenamiento_avatares_posible);
    }

    /**
     * crearListaModulos: extrae la informacion de los modulos de este dispositivo, si no existen le pide al usuario que llene el
     * formulario, y despues crea la lista con la informacion que se mostrara en pantalla
     */
    private void crearListaModulos() {
        if (almacenamiento_modulos_posible) {
            if (new File(directorio_modulos).list().length == 0) {
                Intent intent = new Intent(this, EditarModulos.class);
                intent.putExtra("nombre_dispositivo", nombre_dispositivo);
                intent.putExtra("nombre_modulo_1", getString(R.string.hint_nombres_modulos_1));
                intent.putExtra("nombre_modulo_2", getString(R.string.hint_nombres_modulos_2));
                intent.putExtra("nombre_modulo_3", getString(R.string.hint_nombres_modulos_3));
                intent.putExtra("directorio_modulos", directorio_modulos);
                intent.putExtra("almacenamiento_modulos_posible", almacenamiento_modulos_posible);
                intent.putExtra("configuracion_inicial", true);
                startActivityForResult(intent, REQUEST_CODE_EDITAR_MODULOS);
            }

            File archivo_nombres_modulos = new File(directorio_modulos + File.separator + getString(R.string.nombre_archivo_nombres_modulos));
            String texto_archivo_nombres[] = new String[NUMERO_MODULOS];
            FileInputStream stream;
            try {                                                               //Leemos la informacion guardada de los nombres
                stream = new FileInputStream(archivo_nombres_modulos);
                StringBuilder constructor_texto_leido = new StringBuilder();
                int leido;
                while ((leido = stream.read()) != -1){
                    constructor_texto_leido.append((char)leido);
                }
                texto_archivo_nombres = constructor_texto_leido.toString().split("\n");
                stream.close();
            } catch (Exception e) {
                Log.e(Util.TAG_DSA, "No se pueden extraer los nombres de los modulos: " + e.toString());
            }

            Bitmap[] imagen_modulos = obtenerImagenesModulos(); //Ponemos los nombres y las imagenes en listas
            System.arraycopy(texto_archivo_nombres, 0, nombre_modulos, 0, NUMERO_MODULOS);

            adaptador = new AdaptadorListaModulos(this, nombre_modulos, id_dispositivo, imagen_modulos);
            lv_modulos.setAdapter(adaptador);
        }
    }

    private Bitmap[] obtenerImagenesModulos() {

        Bitmap[] imagen_modulos = new Bitmap[NUMERO_MODULOS];
        String directorio_modulo;

        for (int index = 0; index < NUMERO_MODULOS; index++) {
            directorio_modulo = directorio_modulos + File.separator + (index + 1) + getString(R.string.tipo_imagen);
            imagen_modulos[index] = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_modulo, ancho_imagen, ancho_imagen));
        }
        return imagen_modulos;
    }

    /**
     * extraerAvatar: obtiene el bitmap desde el directorio, y lo devuelve con la resolucion total para mostrar una imagen mas grande, se
     * hace en un hilo en segundo plano para evitar bloquear el hilo principal si la imagen es muy grande y tarda mucho en leerse
     * @param directorio_avatares: ruta del directorio donde estan los avatares
     * @param id_dispositivo: id del dispositivo del avatar que se va a mostrar
     * @param almacenamiento_avatares_posible: dice si es posible leer el directorio
     */
    private void extraerAvatar(String directorio_avatares, String id_dispositivo, boolean almacenamiento_avatares_posible) {
        if(almacenamiento_avatares_posible) {
            new AsyncTask<String, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(String... directorio_e_id) {
                    Bitmap avatar;
                    File directorio_avatares = new File(directorio_e_id[0]);
                    String id = directorio_e_id[1];
                    String images[] = directorio_avatares.list();

                    if (Arrays.asList(images).contains(id + getString(R.string.tipo_imagen))){
                        avatar = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_avatares + File.separator + id +
                                        getString(R.string.tipo_imagen), ancho_avatar, ancho_avatar));
                    } else avatar = BitmapFactory.decodeResource(getResources(), R.mipmap.photon);
                    return avatar;
                }
                @Override
                protected void onPostExecute(Bitmap imagen){
                    iv_avatar.setImageBitmap(imagen);
                }
            }.execute(directorio_avatares, id_dispositivo);
        }
    }

    /**
     * actualizarConexion: acutualiza el TextView de la conexion desde una clase externa
     * @param conexion_dispositivo: estado de la conexion
     * @param activity: activity desde la cual se esta llamando, se utiliza para accedes a los recursos de la app
     */
    public static void actualizarDispositivoActual(Boolean conexion_dispositivo, String nombre_dispositivo, Activity activity){
        if(index_dispositivo != -1){
            tv_conexion_dispositivo.setTextColor(ContextCompat.getColor(activity,
                    (conexion_dispositivo ? R.color.verde_online : R.color.rojo_offline)));
            tv_conexion_dispositivo.setText(conexion_dispositivo ? activity.getString(R.string.online) : activity.getString(R.string.offline));
            tv_nombre_dispositivo.setText(nombre_dispositivo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDITAR_MODULOS){
            if (resultCode == RESULT_OK){
                String nombres_modulos_nuevos[] = data.getStringArrayExtra("nuevos_nombres");
                adaptador.setNombresModulos(nombres_modulos_nuevos);
                adaptador.setImagenesModulos(obtenerImagenesModulos());
                adaptador.notifyDataSetChanged();
            }
        } else if (requestCode == REQUEST_CODE_EDITAR_AVATAR){
            if (resultCode == RESULT_OK) {
                try{
                    InputStream stream = getContentResolver().openInputStream(data.getData());
                    final Bitmap imagen_nueva = BitmapFactory.decodeStream(stream, new Rect(0, 0, ancho_avatar, ancho_avatar), new BitmapFactory.Options());
                    if (stream != null) stream.close();

                    //Intentamos guardar la imagen en otro hilo, ya que si la imagen es pesada estre proceso podria tardar
                    //Utilizamos AsyncTask, clase recomendada por Android para crear nuevos hilos, documentacion en:
                    //http://developer.android.com/intl/es/reference/android/os/AsyncTask.html
                    new AsyncTask<Bitmap, Void, Boolean>(){

                        @Override
                        protected Boolean doInBackground(Bitmap... imagenes) {
                            File ruta_imagen = new File(directorio_avatares + File.separator + id_dispositivo + getString(R.string.tipo_imagen));
                            FileOutputStream archivo;
                            try {
                                archivo = new FileOutputStream(ruta_imagen);
                                if (imagenes[0].getByteCount() > Util.MAX_TAM_IMAGEN_AVATAR) {
                                    imagenes[0].compress(Bitmap.CompressFormat.JPEG, (Util.MAX_TAM_IMAGEN_AVATAR * 100 /
                                            (imagenes[0].getByteCount())), archivo);
                                } else imagenes[0].compress(Bitmap.CompressFormat.JPEG, 100, archivo);
                                archivo.close();
                                Log.i(Util.TAG_DSA, "Se cambio el avatar exitosamente");
                            } catch(Exception e){
                                Log.e(Util.TAG_DSA, "No se completo exitosamente el guardado de imagen");
                                return false;
                            }
                            return true;
                        }

                        @Override
                        protected  void onPostExecute(Boolean resultado){
                            if (!resultado){
                                Util.toast(DispositivoSeleccionadoActivity.this, getString(R.string.guardado_no_exitoso));
                            } else {
                                iv_avatar.setImageBitmap(Util.cortarImagenCircuilar(imagen_nueva));
                                avatar_cambiado = true;
                            }
                        }
                    }.execute(imagen_nueva);
                }catch (Exception e){
                    Log.e(Util.TAG_DSA, e.toString());
                }
            }
        }
    }

    /**
     * getIndexActual: devuelve la posicion en la lista de dispositivos del dispositivo que se esta mostrando actualmente
     * @return entero quer representa la posicion
     */
    public static Integer getIndexActual(){
        return index_dispositivo;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        index_dispositivo = null;
        if (avatar_cambiado) {
            setResult(RESULT_OK);
            avatar_cambiado = false;
        } else setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
