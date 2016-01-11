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
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;

/**
 * Clase DispositivosActivity: muestra la lista con los dispositivos Photon asociados a la cuenta que esta iniciada y muestra
 * sus estados de conexion
 * Created by LUIS ALFONSO on 17/12/2015.
 */
public class DispositivosActivity extends AppCompatActivity {

    private final static String Tag = "SP DispositivosA";

    private static final int REQUEST_CODE = 1215;                   //Codigo para el intent que permitira al usuario elegir un avatar
    private static final int MAX_TAM_IMAGEN = 6000000;              //Maximo tamanio en bits que puede cubrir un avatar
    private static final double RELACION_WIDTH_AVATARES = 1.0 / 3;  //Relacion de los avatares respecto a la pantalla del smartphone
    private static final boolean CONECTADO = true;
    private static final boolean DESCONECTADO = false;

    private ArrayList<String> nombres_dispositivos; //Informacion de los dispositivos y del almacenamiento en el smartphone del usuario
    private ArrayList<String> ids_dispositivos;
    private ArrayList<Boolean> conexion_dispositivos;
    private boolean almacenamiento_avatares_posible = false;
    private File directorio_avatares;
    private File directorio_app;
    private int numero_dispositivos;

    private AdaptadorListaDispositivos adaptador;

    private static HiloActualizarDispositivos hilo_actualizar_dispositivos;  //Runnable que correra sobre otro hilo para actualizar la conexion

    private int index_imagen_a_reemplazar;
    private int ancho_avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos);

        capturarInformacionSesion();
        prepararAvatares();
        crearListaDispositivos();
        iniciarEventoBotonHome();
        iniciarHiloActualizacionConexiones();
    }

    @Override
    protected void onDestroy() {
        if(isFinishing()) {
            hilo_actualizar_dispositivos.interrumpir();
            Async.executeAsync(SparkCloud.get(getApplicationContext()), new Async.ApiWork<SparkCloud, Void>() {
                @Override
                public Void callApi(SparkCloud sparkCloud) throws SparkCloudException, IOException {
                    SparkCloud.get(DispositivosActivity.this).logOut();
                    return null;
                }

                @Override
                public void onSuccess(Void v) {
                }

                @Override
                public void onFailure(SparkCloudException exception) {
                    Log.e(Tag, "No se puede cerrar sesion de Particle");
                }
            });
        }
        super.onDestroy();
    }

    /**
     * capturarInformacionSesion: captura el intent de la activity anterior y recibimos la informacion
     */
    private void capturarInformacionSesion() {
        ArrayList<String> conexion_dispositivos_string;

        Intent intent = getIntent();                //Capturamos la informacion que se envio de la activity IniciarSesionActivity
        directorio_app = new File(intent.getStringExtra("directorio_app"));
        nombres_dispositivos = intent.getStringArrayListExtra("nombres_dispositivos");
        ids_dispositivos = intent.getStringArrayListExtra("ids_dispositivos");
        conexion_dispositivos_string = intent.getStringArrayListExtra("conexion_dispositivos");
        directorio_avatares = new File(directorio_app + File.separator + "Avatares");
        almacenamiento_avatares_posible = Util.comprobarDirectorio(this, directorio_avatares);
        numero_dispositivos = nombres_dispositivos.size();

        conexion_dispositivos = new ArrayList<>();
        for (String conexion: conexion_dispositivos_string)
            conexion_dispositivos.add(conexion.equals(getString(R.string.online)) ? CONECTADO: DESCONECTADO);
    }

    /**
     * prepararAvatares: calcula el ancho adecuado de los avatares dependiendo del dispositivo smartphone que se utilice
     */
    private void prepararAvatares() {
        DisplayMetrics metrics = new DisplayMetrics();                  //Obtenemos las medidas de la pantalla del celular
        getWindowManager().getDefaultDisplay().getMetrics(metrics);     //para ajustar lo ancho de las imagenes
        ancho_avatar = (int)(metrics.widthPixels * RELACION_WIDTH_AVATARES);
    }

    /**
     * crearListaDispositivos: crea las vistas de la lista de dispositivos de la sesion iniciada con la informacion capturada del
     * servidor de Particle y crea un evento para iniciar la activity DispositivoSeleccionadoActivity cuando pulsen un dispositivo
     */
    private void crearListaDispositivos() {
        ListView dispositivos = (ListView) findViewById(R.id.lvDispositivos);
        adaptador = new AdaptadorListaDispositivos(DispositivosActivity.this, nombres_dispositivos, conexion_dispositivos,
                obtenerAvatares(ids_dispositivos));
        dispositivos.setAdapter(adaptador);

        dispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {     //Si pulsan algun dispositivo en la lista
            @Override
            //(mientras que no sea el avatar)
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((View) (view.getParent())).setBackgroundColor(ContextCompat.getColor(DispositivosActivity.this, R.color.gris_seleccion));
                Intent intent = new Intent(getApplicationContext(), DispositivoSeleccionadoActivity.class);
                intent.putExtra("nombre_dispositivo", nombres_dispositivos.get(position));
                intent.putExtra("id_dispositivo", ids_dispositivos.get(position));
                intent.putExtra("conexion_dispositivo", conexion_dispositivos.get(position));
                intent.putExtra("directorio_app", directorio_app.toString());
                intent.putExtra("directorio_avatares", directorio_avatares.toString());
                intent.putExtra("almacenamiento_avatares_posible", almacenamiento_avatares_posible);
                intent.putExtra("index_dispositivo", position);
                startActivity(intent);
                Log.i(Tag, "Se pulso al dispositivo numero: " + position + ". Iniciando activity DispositivoSeleccionadoActivity");
                ((View) (view.getParent())).setBackgroundColor(ContextCompat.getColor(DispositivosActivity.this, R.color.blanco));
            }
        });

        dispositivos.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);        //Si mantienen pulsado un dispositivo, aparece la opcion de
        dispositivos.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {     //elegir mas y eliminarlos
            boolean dispositivos_seleccionados[] = new boolean[nombres_dispositivos.size()];
            ArrayList<String> ids_dispositivos_seleccionados;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                dispositivos_seleccionados[position] = checked;
                adaptador.sombrear(checked, position);
                adaptador.notifyDataSetChanged();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                for (int index = 0; index < nombres_dispositivos.size(); index++)
                    dispositivos_seleccionados[index] = false;
                MenuInflater creador_menu = mode.getMenuInflater();
                creador_menu.inflate(R.menu.menu_context, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.borrar) {
                    ids_dispositivos_seleccionados = new ArrayList<>();
                    for (int index = 0; index < dispositivos_seleccionados.length; index++) {
                        if (dispositivos_seleccionados[index]) {
                            ids_dispositivos_seleccionados.add(ids_dispositivos.get(index));
                        }
                    }

                    Async.executeAsync(SparkCloud.get(DispositivosActivity.this), new Async.ApiWork<SparkCloud, ArrayList<SparkDevice>>() {
                        @Override
                        public ArrayList<SparkDevice> callApi(SparkCloud sparkCloud) throws SparkCloudException, IOException {
                            ArrayList<SparkDevice> dispositivos_eliminar = new ArrayList<>();
                            for (String id : ids_dispositivos_seleccionados)
                                dispositivos_eliminar.add(sparkCloud.getDevice(id));
                            return dispositivos_eliminar;
                        }

                        @Override
                        public void onSuccess(ArrayList<SparkDevice> dispositivos_eliminar) {
                            for (SparkDevice dispositivo : dispositivos_eliminar)
                                try {
                                    dispositivo.unclaim();
                                } catch (SparkCloudException e) {
                                    Util.toast(DispositivosActivity.this, DispositivosActivity.this.getString(R.string.error_borrar_dispositivos));
                                    Log.e(Tag, "No se pudo eliminar el dispositivo: " + e.getBestMessage());
                                }
                        }

                        @Override
                        public void onFailure(SparkCloudException exception) {
                            Util.toast(DispositivosActivity.this, DispositivosActivity.this.getString(R.string.error_borrar_dispositivos));
                            Log.e(Tag, "No se pudo eliminar el dispositivo: " + exception.toString());
                        }
                    });
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adaptador.sombrearTodos(false);
                adaptador.notifyDataSetChanged();
            }
        });
    }

    /**
     * iniciarEventoBotonHome: crea un evento con las acciones a realizar cuando se pulse el boton home
     */
    private void iniciarEventoBotonHome() {
        final ImageButton boton_home = (ImageButton)findViewById(R.id.ibHome);
        boton_home.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn_presionado));
                }
                if (event.getX() < 0 || event.getY() < 0 || event.getX() > boton_home.getWidth() || event.getY() > boton_home.getHeight()) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                }
                return true;
            }
        });
    }

    /**
     * iniciarHiloActualizacionConexiones: inicia un hilo en segundo plano que actualice los estados de conexion cada 5 segundos
     * y lo mantiene activo y actualizado siempre que se destruya la activity a menos que el usuario se salga de la app
     */
    private void iniciarHiloActualizacionConexiones() {
        if (hilo_actualizar_dispositivos == null) {                             //Considerando que cada vez que giramos nuestro celular y se
            hilo_actualizar_dispositivos = new HiloActualizarDispositivos(this);    //cambia el modo de visión de la app (landscape) se reinicia
            new Thread(hilo_actualizar_dispositivos).start();                   //la activity de cero, es importante que el hilo no se reinicie
            Log.i(Tag, "Se inicio el hilo para actualizar la conexion");    //por lo que si no es nulo, no lo volvemos a iniciar
        }
        else hilo_actualizar_dispositivos.setActivity(this);                    //Solamente acualizamos la activity nueva en la clase del hilo
    }

    /**
     * preguntarUsuario: pregunta al usuario si desea cambiar de avatar. Si se pulsa que si, le permite seleccionar una imagen
     * de su galeria y sustituye el antiguo avatar con esa imagen
     */
    public void preguntarUsuario(int avatar_pulsado){
        index_imagen_a_reemplazar = avatar_pulsado;
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
                startActivityForResult(intent, REQUEST_CODE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)    //Si se contesta nuestro intent
            try {
                InputStream stream = getContentResolver().openInputStream(data.getData());
                Bitmap imagen_nueva = BitmapFactory.decodeStream(stream, new Rect(0, 0, ancho_avatar, ancho_avatar), new BitmapFactory.Options());
                if (stream != null) stream.close();

                //Intentamos guardar la nueva imagen en otro hilo ya que si la imagen es pesada este proceso podria tardar
                //Utilizamos AsynkTask, clase recomendada por Android para crear nuevos hilos, documentacion en:
                //http://developer.android.com/intl/es/reference/android/os/AsyncTask.html
                final String id = ids_dispositivos.get(index_imagen_a_reemplazar);
                final Bitmap imagen_vieja = adaptador.getAvatarDispositivo(index_imagen_a_reemplazar);  //Obtenemos la imagen vieja
                adaptador.setAvatarDispositivo(Util.cortarImagenCircuilar(imagen_nueva), index_imagen_a_reemplazar);                //Ponemos la nueva imagen
                adaptador.notifyDataSetChanged();

                new AsyncTask<Bitmap, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Bitmap... imagenes) {
                        File ruta_imagen = new File(directorio_avatares + File.separator + id + getString(R.string.tipo_imagen));
                        FileOutputStream archivo;
                        try {
                            archivo = new FileOutputStream(ruta_imagen);
                            if (imagenes[0].getByteCount() > MAX_TAM_IMAGEN){
                                imagenes[0].compress(Bitmap.CompressFormat.JPEG, (MAX_TAM_IMAGEN*100/imagenes[0].getByteCount()), archivo);
                            }
                            else imagenes[0].compress(Bitmap.CompressFormat.JPEG, 100, archivo);
                            archivo.close();
                            Log.i(Tag, "Se cambio de avatar exitosamente");
                        } catch (Exception e) {
                            Log.e(Tag, "No se completo exitosamente el guardado de imagen");
                            return false;
                        }
                        return true;
                    }
                    @Override
                    protected void onPostExecute(Boolean resultado){
                        if (!resultado) {       //Si no se guardo exitosamente avisamos al usuario y ponemos la imagen vieja
                            adaptador.setAvatarDispositivo(Util.cortarImagenCircuilar(imagen_vieja), index_imagen_a_reemplazar);
                            Util.toast(DispositivosActivity.this, getString(R.string.guardado_no_exitoso));
                        }
                    }
                }.execute(imagen_nueva);
            } catch (Exception e) {
                Log.e(Tag, e.toString());
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * obtenerAvatares: funcion que extrae los avatares que el usuario habia preestablecido la ultima vez que los modifico
     * @param ids_dispositivos: ids de los dispositivos Photon, ya que las imagenes se almacenan con los ids
     * @return arreglo que contiene los avatares en el mismo orden que los dispositivos
     */
    private Bitmap[] obtenerAvatares(ArrayList<String> ids_dispositivos){
        Bitmap avatares_dispositivos[] = new Bitmap[numero_dispositivos];
        if (almacenamiento_avatares_posible){
            String images[] = directorio_avatares.list();      //Obtenemos los archivos dentro del directorio de avatares

            int count = 0;
            for (String id_dispositivo: ids_dispositivos){
                if (Arrays.asList(images).contains(id_dispositivo + getString(R.string.tipo_imagen))){
                    avatares_dispositivos[count++] = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_avatares + File.separator +
                            id_dispositivo + getString(R.string.tipo_imagen), ancho_avatar, ancho_avatar));
                } else avatares_dispositivos[count++] = Util.cortarImagenCircuilar(BitmapFactory.decodeResource(getResources(), R.mipmap.photon));
            }
        }
        else{
            for (int index = 0; index < numero_dispositivos; index++)
                avatares_dispositivos[index] = null;
        }
        return avatares_dispositivos;
    }

    /**
     * actualizarConexiones: funcion que se llama desde una clase externa para actualizar el estado de conexion de los dispositivos
     * en la interfaz grafica, tanto en esta activity como en la activity DispositivoSeleccionadoActivity
     * @param conexiones: lista de tamano igual al numero de dispositivos del usuario con los nuevos estados de conexion actualizados
     */
    public void actualizarDispositivos(ArrayList<String> nombres, ArrayList<Boolean> conexiones){
        adaptador.setNombresDispositivos(nombres);
        adaptador.setConexionesDispositivos(conexiones);
        adaptador.notifyDataSetChanged();

        for (int index = 0; index < numero_dispositivos; index++) {     //Actualizamos los ArrayList
            conexion_dispositivos.set(index, conexiones.get(index));
            nombres_dispositivos.set(index, nombres.get(index));
        }

        if (DispositivoSeleccionadoActivity.getIndexActual() != null)
            DispositivoSeleccionadoActivity.actualizarDispositivoActual(conexion_dispositivos.get(DispositivoSeleccionadoActivity.getIndexActual()),
                    nombres_dispositivos.get(DispositivoSeleccionadoActivity.getIndexActual()) ,this);
        Log.v("Actualizacion Conexion", "Se han actualizado los estados de conexion de dispositivos en la UI");
    }

    @Override
    public void onBackPressed() {       //Cuando el usuario presione back, se inicia la activity de inicio de sesion y se destruye esta
        AlertDialog.Builder dialogo_cerrar_sesion = new AlertDialog.Builder(this);        //para cerrar su sesion en Particle
        dialogo_cerrar_sesion.setTitle(R.string.dialogo_cerrar_sesion);
        dialogo_cerrar_sesion.setCancelable(false);
        dialogo_cerrar_sesion.setMessage(R.string.mensaje_dialogo_cerrar_sesion);
        dialogo_cerrar_sesion.setPositiveButton(R.string.dialogo_cerrar_sesion_si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), IniciarSesionActivity.class);
                startActivity(intent);
                finish();
            }
        });
        dialogo_cerrar_sesion.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert_dialogo = dialogo_cerrar_sesion.create();
        alert_dialogo.show();
    }
}