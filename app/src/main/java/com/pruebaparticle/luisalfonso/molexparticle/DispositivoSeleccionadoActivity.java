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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

    private Menu menu;
    private boolean menu_cambio_nombre_modulo = false;      //Banderas que indican si el usuario esta editando el nombre de
    private boolean menu_cambio_nombre_dispositivo = false; //un modulo o del dispositivo
    private int posicion_modulo_imagen_cambiada;

    private String id_dispositivo;              //Informacion del dispositivo enviada por la activity DispositivosActivity
    private String nombre_dispositivo;
    private Boolean conexion_dispositivo;
    private String directorio_avatares;
    private boolean almacenamiento_avatares_posible;

    private boolean almacenamiento_modulos_posible;
    private  String directorio_modulos;

    private static Integer index_dispositivo = null;
    private static TextView tv_conexion_dispositivo;
    private static TextView tv_nombre_dispositivo;
    private ImageView iv_avatar;

    private int ancho_imagen;
    private int ancho_avatar;

    private static boolean avatar_cambiado = false;

    private AdaptadorListaModulos adaptador;

    private int seleccion = Util.SELECCION_CAMARA; //Variable que almacena la seleccion del usuario para abrir galeria o camara
    private File ruta_imagen_camara;               //Ruta deonde la camara almacena la fotografia tomada

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_dispositivo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.iListo:
                item.setVisible(false);
                if (menu_cambio_nombre_modulo) {
                    menu_cambio_nombre_modulo = false;
                    adaptador.cambioNombreListo();
                    guardarNombresModulos();
                } else{
                    cambioNombreDispositivoListo();
                }
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
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
        directorio_modulos = Util.getDirectorioApp() + File.separator + "Modulos" + File.separator + id_dispositivo;
        almacenamiento_avatares_posible = intent.getBooleanExtra("almacenamiento_avatares_posible", false);
        almacenamiento_modulos_posible = Util.comprobarDirectorio(this, new File(directorio_modulos));
    }

    /**
     * obtenerVistas: obtiene las vistas necesarias para las variables globales de esta clase
     */
    private void obtenerVistas() {
        tv_conexion_dispositivo = (TextView)findViewById(R.id.tvConexionDispositivoGrande);
        tv_nombre_dispositivo = (TextView)findViewById(R.id.tvNombreDispositivoGrande);
        tv_nombre_dispositivo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View texto_nombre_dispositivo) {
                if (menu_cambio_nombre_modulo) {
                    menu_cambio_nombre_modulo = false;
                    adaptador.cambioNombreListo();
                }
                String hint = ((TextView)texto_nombre_dispositivo).getText().toString();
                EditText etNombre_dispositivo = (EditText) findViewById(R.id.etNombreDispositivoGrande);
                etNombre_dispositivo.setHint(hint);
                texto_nombre_dispositivo.setVisibility(View.GONE);
                etNombre_dispositivo.setVisibility(View.VISIBLE);
                mostrarItemListo();
                menu_cambio_nombre_dispositivo = true;
                return true;
            }
        });
        iv_avatar = (ImageView)findViewById(R.id.ivAvatarGrande);
    }

    /**
     * prepararImagenesModulos: obtiene las medidas de la pantalla del smartphone utilizado y calcula los pixeles a los que se deben
     * escalar las imagenes para dar una buena apariencia
     */
    private void prepararImagenesModulos() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ancho_avatar = (int)(metrics.widthPixels * Util.RELACION_WIDTH_AVATAR);
        ancho_imagen = (int)(metrics.widthPixels * Util.RELACION_WIDTH_IMAGEN);
    }

    /**
     * actualizarVistas: actualiza las vistas del layout de esta activity con la informacion obtenida
     */
    private void actualizarVistas() {
        tv_nombre_dispositivo.setText(nombre_dispositivo);
        tv_conexion_dispositivo.setTextColor(ContextCompat.getColor(this, conexion_dispositivo ? R.color.verde_online : R.color.rojo_offline));
        tv_conexion_dispositivo.setText(conexion_dispositivo ? getString(R.string.online) : getString(R.string.offline));
        extraerAvatar();
    }

    /**
     * crearListaModulos: extrae la informacion de los modulos de este dispositivo, si no existen le pide al usuario que llene el
     * formulario, y despues crea la lista con la informacion que se mostrara en pantalla
     */
    private void crearListaModulos() {
        if (almacenamiento_modulos_posible) {
            Bitmap[] imagen_modulos = obtenerImagenesModulos(); //Ponemos los nombres y las imagenes en listas

            adaptador = new AdaptadorListaModulos(this, obtenerNombresModulos(), id_dispositivo, imagen_modulos);
            ((ListView)findViewById(R.id.lvModulos)).setAdapter(adaptador);
        }
    }

    /**
     * cambiarAvatar: se llama al pulsar el avatar, lanza un dialogo que le permite al usuario elegir si desea cambiar su
     * avatar tomando una foto o seleccionando una imagen de su galeria
     * @param imagen: vista de la imagen del avatar
     */
    public void cambiarAvatar(View imagen){
        Util.vibrar(this);
        seleccion = Util.SELECCION_CAMARA;
        AlertDialog.Builder dialogo_seleccion = new AlertDialog.Builder(DispositivoSeleccionadoActivity.this);
        CharSequence items[] = new CharSequence[]{getString(R.string.opcion_avatar_1), getString(R.string.opcion_avatar_2)};
        dialogo_seleccion.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int posicion) {
                switch (posicion) {
                    case Util.SELECCION_CAMARA:
                        seleccion = Util.SELECCION_CAMARA;
                        break;
                    case Util.SELECCION_GALERIA:
                        seleccion = Util.SELECCION_GALERIA;
                        break;
                }
            }
        });
        dialogo_seleccion.setNegativeButton(getString(R.string.cancelar), null);
        dialogo_seleccion.setTitle(getString(R.string.dialogo_cambiar_imagen));
        dialogo_seleccion.setPositiveButton(getString(R.string.dialogo_cambiar_imagen_si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (seleccion) {
                    case Util.SELECCION_CAMARA:
                        ruta_imagen_camara = new File(Util.solicitarFotoCamara(DispositivoSeleccionadoActivity.this,
                                (directorio_avatares), id_dispositivo));
                        break;
                    case Util.SELECCION_GALERIA:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, Util.REQUEST_CODE_EDITAR_AVATAR);
                        break;
                }
            }
        });
        dialogo_seleccion.show();
    }

    /**
     * obtenerNombresModulos: funcion que lee en el almacenamiento externo los nombres de los modulos que el usuario puso a este
     * dispositivo y guarda los Strings en la variable nombre_modulos
     */
    private String[] obtenerNombresModulos() {
        String nombre_modulos[] = new String[Util.NUMERO_MODULOS];
        File archivo_nombres_modulos = new File(directorio_modulos + File.separator + getString(R.string.nombre_archivo_nombres_modulos));
        FileInputStream stream;
        try {                                                               //Leemos la informacion guardada de los nombres
            stream = new FileInputStream(archivo_nombres_modulos);
            StringBuilder constructor_texto_leido = new StringBuilder();
            int leido;
            while ((leido = stream.read()) != -1) constructor_texto_leido.append((char)leido);
            nombre_modulos = constructor_texto_leido.toString().split("\n");
            stream.close();
        } catch (Exception e) {
            Log.e(Util.TAG_DSA, "No se pueden extraer los nombres de los modulos: " + e.toString());
        }
        return nombre_modulos;
    }

    /**
     * obtenerImagenesModulos: lee del almacenamiento externo las imagenes que el usuario puso a cada modulo de este dispositivo
     * y las regresa ya cortadas de forma circular
     * @return arreglo de bitmaps con las tres imagenes de los modulos
     */
    private Bitmap[] obtenerImagenesModulos() {
        Bitmap[] imagen_modulos = new Bitmap[Util.NUMERO_MODULOS];
        String directorio_modulo;

        for (int index = 0; index < Util.NUMERO_MODULOS; index++) {
            directorio_modulo = directorio_modulos + File.separator + (index + 1) + getString(R.string.tipo_imagen);
            imagen_modulos[index] = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_modulo, ancho_imagen, ancho_imagen));
        }
        return imagen_modulos;
    }

    /**
     * extraerAvatar: obtiene el bitmap desde el directorio, y lo devuelve con la resolucion total para mostrar una imagen mas grande, se
     * hace en un hilo en segundo plano para evitar bloquear el hilo principal si la imagen es muy grande y tarda mucho en leerse
     */
    private void extraerAvatar() {
        if(almacenamiento_avatares_posible) {
            new AsyncTask<Void, Void, Bitmap>() {     //Extraemos el avatar en otro hilo por si tarda mucho la extraccion
                @Override
                protected Bitmap doInBackground(Void... aVoid) {
                    Bitmap avatar;
                    String images[] = new File(directorio_avatares).list();

                    if (Arrays.asList(images).contains(id_dispositivo + getString(R.string.tipo_imagen))){
                        avatar = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_avatares + File.separator + id_dispositivo +
                                        getString(R.string.tipo_imagen), ancho_avatar, ancho_avatar));
                    } else avatar = BitmapFactory.decodeResource(getResources(), R.mipmap.photon);
                    return avatar;
                }
                @Override
                protected void onPostExecute(Bitmap imagen){
                    iv_avatar.setImageBitmap(imagen);
                }
            }.execute();
        }
    }

    /**
     * actualizarConexion: acutualiza el TextView de la conexion desde una clase externa
     * @param conexion_dispositivo: estado de la conexion
     * @param activity: activity desde la cual se esta llamando, se utiliza para accedes a los recursos de la app
     */
    public static void actualizarDispositivoActual(Boolean conexion_dispositivo, String nombre_dispositivo, Activity activity){
        if(index_dispositivo != null){
            tv_conexion_dispositivo.setTextColor(ContextCompat.getColor(activity,
                    (conexion_dispositivo ? R.color.verde_online : R.color.rojo_offline)));
            tv_conexion_dispositivo.setText(conexion_dispositivo ? activity.getString(R.string.online) : activity.getString(R.string.offline));
            tv_nombre_dispositivo.setText(nombre_dispositivo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK){
            switch (requestCode){
                case Util.REQUEST_CODE_EDITAR_MODULO:
                    try{            //Si se cambia la imagen de un modulo extraemos la nueva imagen y actualizamos las vistas
                        InputStream stream = getContentResolver().openInputStream(data.getData());
                        Bitmap imagen_nueva = BitmapFactory.decodeStream(stream, new Rect(0, 0, ancho_imagen, ancho_imagen), new BitmapFactory.Options());
                        if (stream != null) stream.close();
                        guardarYCambiarImagenModulo(imagen_nueva);
                    } catch (Exception e) {
                        Log.e(Util.TAG_DSA, "No se puede abrir la imagen:" + e.getMessage());
                    }
                    break;
                case Util.REQUEST_CODE_EDITAR_AVATAR:
                    try{            //Si se cambia el avatar del dispositivo extraemos la nueva imagen y actualizamos la vista
                        InputStream stream = getContentResolver().openInputStream(data.getData());
                        Bitmap imagen_nueva = BitmapFactory.decodeStream(stream, new Rect(0, 0, ancho_avatar, ancho_avatar), new BitmapFactory.Options());
                        if (stream != null) stream.close();
                        guardarYCambiarImagenAvatar(imagen_nueva);
                    }catch (Exception e){
                        Log.e(Util.TAG_DSA, e.toString());
                    }
                    break;
                case Util.REQUEST_FOTO_CAMARA:
                    try{            //Si se cambia la imagen de un modulo extraemos la nueva imagen y actualizamos las vistas
                        Bitmap imagen_nueva = BitmapFactory.decodeFile(ruta_imagen_camara.toString());
                        guardarYCambiarImagenAvatar(imagen_nueva);
                        if (!ruta_imagen_camara.delete())
                            Log.w(Util.TAG_DSA, "No se pudo eliminar la foto con la calidad completa");
                    } catch (Exception e) {
                        Log.e(Util.TAG_DSA, "No se puede abrir la imagen:" + e.getMessage());
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * guardarYCambiarImagenAvatar: guarda el nuevo avatar en el almacenamiento externo y lo situa en su vista de la UI
     * @param imagen_nueva: nuevo avatar seleccionado
     */
    private void guardarYCambiarImagenAvatar(final Bitmap imagen_nueva) {
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
    }

    /**
     * guardarYCambiarImagenModulo: guarda la nueva imagen de modulo en el almacenamiento externo y actualiza la vista del modulo
     * para mostrar la nueva imagen
     * @param imagen_nueva: nueva imagen del modulo
     */
    private void guardarYCambiarImagenModulo(Bitmap imagen_nueva) {
        new AsyncTask<Bitmap, Void, Boolean>(){
            @Override
            protected Boolean doInBackground(Bitmap... imagenes) {
                File ruta_imagen = new File(directorio_modulos + File.separator + (posicion_modulo_imagen_cambiada + 1) +
                        getString(R.string.tipo_imagen));
                FileOutputStream archivo;
                try {
                    archivo = new FileOutputStream(ruta_imagen);
                    if (imagenes[0].getByteCount() > Util.MAX_TAM_IMAGEN_MODULO) {
                        imagenes[0].compress(Bitmap.CompressFormat.JPEG, (Util.MAX_TAM_IMAGEN_MODULO * 100 /
                                (imagenes[0].getByteCount())), archivo);
                    } else imagenes[0].compress(Bitmap.CompressFormat.JPEG, 100, archivo);
                    archivo.close();
                    Log.i(Util.TAG_DSA, "Se cambio la imagen de modulo exitosamente");
                } catch(Exception e){
                    Log.e(Util.TAG_DSA, "No se completo exitosamente el guardado de imagen");
                    return false;
                }
                return true;
            }
            @Override
            protected  void onPostExecute(Boolean resultado){
                if (resultado){
                    adaptador.setImagenesModulos(obtenerImagenesModulos());
                    adaptador.notifyDataSetChanged();
                } else{
                    Util.toast(DispositivoSeleccionadoActivity.this, DispositivoSeleccionadoActivity.this.getString(R.string.cambios_no_guardados));
                }
            }
        }.execute(imagen_nueva);
    }

    /**
     * getIndexActual: devuelve la posicion en la lista de dispositivos del dispositivo que se esta mostrando actualmente
     * @return entero quer representa la posicion
     */
    public static Integer getIndexActual(){
        return index_dispositivo;
    }

    /**
     * guardarNombresModulos: almacena los nombres de los modulos en un archivo txt para poder acceder despues a ellos
     */
    private void guardarNombresModulos(){
        if (almacenamiento_modulos_posible) {
            File documento_con_nombres = new File(directorio_modulos, getString(R.string.nombre_archivo_nombres_modulos));
            FileOutputStream stream;        //Guardamos un txt con los nombres de los modulos en el orden correcto para facilitar la lectura
            String nombres_modulos_nuevos[] = adaptador.getNombresModulos();
            try {
                stream = new FileOutputStream(documento_con_nombres);
                stream.write((nombres_modulos_nuevos[0] + "\n").getBytes());
                stream.write((nombres_modulos_nuevos[1] + "\n").getBytes());
                stream.write((nombres_modulos_nuevos[2]).getBytes());
                stream.close();
            } catch (Exception e) {
                Log.e(Util.TAG_EM, e.toString());
                Util.toast(this, getString(R.string.cambios_no_guardados));
            }
        }
    }

    /**
     * menuCambioNombreModulo: se llama al hacer un click largo en un modulo y muestra el menu de cambio de nombre, que cuenta
     * con un item "listo" para almacenar los nuevos nombres que se introducen
     */
    public void menuCambioNombreModulo(){
        if (menu_cambio_nombre_dispositivo){
            cambioNombreDispositivoListo();
        }
        menu_cambio_nombre_modulo = true;
        mostrarItemListo();
    }

    /**
     * mostrarItemListo: muestra el item "listo" en el menu
     */
    private void mostrarItemListo() {
        menu.findItem(R.id.iListo).setVisible(true);
    }

    /**
     * esconderItemListo: quita el item "listo" del menu
     */
    private void esconderItemListo() {
        menu.findItem(R.id.iListo).setVisible(false);
    }

    /**
     * cambioNombreDispositivoListo: se llama si se completo de forma exitosa el cambio de nombre del dispositivo para que
     * lo almacene en la nubre de Particle
     */
    private void cambioNombreDispositivoListo() {
        menu_cambio_nombre_dispositivo = false;
        EditText etNombre_dispositivo = (EditText)findViewById(R.id.etNombreDispositivoGrande);
        String nuevo_nombre = etNombre_dispositivo.getText().toString();
        if (nuevo_nombre.isEmpty()) {
            cambioNombreDispositivoCancelado();
            return;
        }
        Util.ParticleAPI.cambiarNombreDispositivo(id_dispositivo, nuevo_nombre);
        tv_nombre_dispositivo.setText(nuevo_nombre);
        etNombre_dispositivo.setVisibility(View.GONE);
        tv_nombre_dispositivo.setVisibility(View.VISIBLE);
    }

    /**
     * cambioNombreDispositivoCancelado: se llama si se cancela el cambio de nombre del dispositivo para poner el nombre
     * que tenia anteriormente
     */
    private void cambioNombreDispositivoCancelado() {
        menu_cambio_nombre_dispositivo = false;
        EditText etNombre_dispositivo = (EditText)findViewById(R.id.etNombreDispositivoGrande);
        etNombre_dispositivo.setText(null);
        etNombre_dispositivo.setVisibility(View.GONE);
        tv_nombre_dispositivo.setVisibility(View.VISIBLE);
    }

    /**
     * cambioImagenModulo: se llama al hacer un click largo sobre la imagen de un modulo
     * @param numero_modulo: posicion del modulo que fue seleccionado
     */
    public void cambioImagenModulo(int numero_modulo) {
        posicion_modulo_imagen_cambiada = numero_modulo;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, Util.REQUEST_CODE_EDITAR_MODULO);
    }

    @Override
    public void onBackPressed() {
        if (menu_cambio_nombre_modulo) {    //Si se presiona back cuando esta activado el contexto de cambio de nombre (se muestra
            esconderItemListo();            //el item "listo"), tan solo se cierra ese contexto, no la activity completa
            menu_cambio_nombre_modulo = false;
            adaptador.cambioNombreCancelado();
        } else if (menu_cambio_nombre_dispositivo) {
            esconderItemListo();
            cambioNombreDispositivoCancelado();
        } else {
            index_dispositivo = null;
            if (avatar_cambiado) {
                setResult(RESULT_OK);
                avatar_cambiado = false;
            } else setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }
}