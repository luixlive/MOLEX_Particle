package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.utils.Async;

/**
 * Clase Util: Contiene funciones publicas y estaticas que se utilizan de manera repetitiva en la app, se agrupan aqui para acceder a
 * ellas de forma estatica y evitar codigo repetitivo.
 * Created by LUIS ALFONSO on 30/12/2015.
 */
public class Util {

    //Tags para los mensajes del LogCat
    private final static String TAG_UTIL = "SP Util";
    public final static String TAG_DSA = "SP DSeleccionadoA";
    public final static String TAG_EM = "SP EditarModulos";
    public final static String TAG_DA = "SP DispositivosA";
    public final static String TAG_HAD = "SP HActualizarD";
    public static final String TAG_ISA = "SP IniciarSesionA";

    //Constantes utilizadas frecuentemente en las clases
    public static final int TIEMPO_VIBRACION = 35;
    public static final  double RELACION_WIDTH_AVATARES = 1.0 / 3;//Relacion ancho de avatar respecto al smartphone
    public final static double RELACION_WIDTH_IMAGEN = 1 / 6.0;   //Igual que el anterior pero de la activity DispositivoSeleccionadoA
    public final static double RELACION_WIDTH_AVATAR = 1 / 2.0;
    public static final int MAX_TAM_IMAGEN_AVATAR = 6000000;      //Maximo tamanio en bits que puede cubrir un avatar
    public final static int MAX_TAM_IMAGEN_MODULO = 2000000;      //Maximo tamanio en bits de la imagen de un modulo
    public final static int NUMERO_MODULOS = 3;                   //Numero de modulos por cada dispositivo SmartPower
    public final static int ERROR = -1;
    public final static int ENCENDIDO = 1;
    public final static int APAGADO = 0;
    public final static int TIEMPO_MUERTO_HILO_CONEXION = 3000;
    public final static int SELECCION_CAMARA = 0;
    public final static int SELECCION_GALERIA = 1;

    public static final boolean CONECTADO = true;
    public static final boolean DESCONECTADO = false;

    public static final int REQUEST_CODE_DISPOSITIVO_SELECCIONADO = 1214;
    public final static int REQUEST_CODE_EDITAR_AVATAR = 2913;
    public final static int REQUEST_CODE_EDITAR_MODULO = 2912;
    public final static int REQUEST_FOTO_CAMARA_AVATAR = 2101;
    public final static int REQUEST_FOTO_CAMARA_MODULO = 2201;
    public final static int REQUEST_VOID = -1;

    private static Vibrator vibrador;
    private static Boolean posible_vibrar = null;
    private static String directorio_app;

    /**
     * vibrar: ocasiona una breve vibracion en el smartphone para indicarle al usuario que se realizo una accion (por
     * ejemplo presionar un boton)
     * @param activity: activity actual en la que se encunetra la app que proporcionara el contexto para la vibracion
     */
    public static void vibrar(Activity activity){
        if (posible_vibrar == null) {
            vibrador = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            posible_vibrar = vibrador.hasVibrator();
        }
        if (posible_vibrar) vibrador.vibrate(TIEMPO_VIBRACION);
    }

    /**
     * solicitarFotoCamara: solicita al sistema que le de la posibilidad al usuario de tomar una fotografia y regresa la
     * ruta donde se almaceno la imagen
     * @param activity: contexto desde el cual se llama a la funcion
     * @param directorio: directorio donde se va a almacenar la imagen
     * @param nombre_imagen: nombre de la imagen (el intent agrega un numero random al final del nombre, por lo que se almacena
     *                     ese nombre nuevo y se retorna toda la ruta con el nuevo nombre)
     * @param request: request que indica si se quiere cambiar un modulo o el avatar
     * @return ruta de la imagen guardada por la camara
     */
    public static String solicitarFotoCamara(Activity activity, String directorio, String nombre_imagen, int request){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagen = null;
        try {
            imagen = File.createTempFile(nombre_imagen, activity.getString(R.string.tipo_imagen), new File(directorio));
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imagen));
        } catch (IOException e) {
            Log.e(TAG_DSA, "No se pudo tomar la foto:" + e.getMessage());
        }
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {    //Comprobamos que haya apps que pueden utilizar este intent
            if (request == REQUEST_CODE_EDITAR_MODULO)
                activity.startActivityForResult(takePictureIntent, REQUEST_FOTO_CAMARA_MODULO);
            else if (request == REQUEST_CODE_EDITAR_AVATAR)
                activity.startActivityForResult(takePictureIntent, REQUEST_FOTO_CAMARA_AVATAR);
        } else toast(activity, activity.getString(R.string.no_camara));
        return imagen == null ? null : imagen.toString();
    }

    /**
     * cortarImagenCircuilar: corta una imagen para que quede con una forma circular (codigo obtenido de la discusion
     * http://stackoverflow.com/questions/11932805/cropping-circular-area-from-bitmap-in-android y modificad y adaptado a esca clase
     * por Luis Alfonso Ch. Abbadie)
     * @param imagen: imagen a recortar
     * @return: resultado, la imagen ya recortada
     */
    public static Bitmap cortarImagenCircuilar(Bitmap imagen){
        if (imagen == null) return null;
        Bitmap resultado = Bitmap.createBitmap(imagen.getWidth(), imagen.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultado);

        final int color = 0xffFFFFFF;
        final Paint pintura = new Paint();
        final Rect rectangulo = new Rect(0, 0, imagen.getWidth(), imagen.getHeight());

        pintura.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        pintura.setColor(color);

        int x;
        int y;
        int diametro_circulo;

        if (imagen.getWidth() < imagen.getHeight()) {
            diametro_circulo = imagen.getWidth();
            canvas.drawCircle(diametro_circulo / 2, imagen.getHeight() / 2, diametro_circulo / 2, pintura);
            x = 0;
            y = (imagen.getHeight()-diametro_circulo)/2;
        }
        else {
            diametro_circulo = imagen.getHeight();
            canvas.drawCircle(imagen.getWidth() / 2, diametro_circulo / 2, diametro_circulo / 2, pintura);
            x = (imagen.getWidth()- diametro_circulo)/2;
            y = 0;
        }
        pintura.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(imagen, rectangulo, rectangulo, pintura);
        return Bitmap.createBitmap(resultado, x, y, diametro_circulo, diametro_circulo);
    }

    /**
     * obtenerImagenReducida: lee la imagen de la ruta establecida como bitmap leyendo solamente la cantidad de pixeles necesarios para
     * que la imagen se vea bien en el area del ancho y alto especificados, asi se ahorra memoria. Checar link en la documentacion de la
     * funcion calcularTamanoNecesarioImagen para mas informacion
     * @param ruta_imagen: ruta donde se encuentra la imagen
     * @param ancho: ancho en pixeles deseados
     * @param alto: alto en pixeles deseados
     * @return: imagen con una cantidad de pixeles optima para no impactar fuertemente en la memoria
     */
    public static Bitmap obtenerImagenReducida(String ruta_imagen, int ancho, int alto) {
        final BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta_imagen, opciones);
        opciones.inSampleSize = calcularTamanoNecesarioImagen(opciones, ancho, alto);
        opciones.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(ruta_imagen, opciones);
    }

    /**
     * Para mas informacion de este metodo y el metodo obtenerImagenReducida, ver la documentacion oficial de Android sobre como evitar
     * desperdicios de memoria al manejar Bitmaps en: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    private static int calcularTamanoNecesarioImagen(BitmapFactory.Options opciones, int ancho_nec, int altura_nec){
        final int altura = opciones.outHeight;
        final int ancho = opciones.outWidth;
        int inTamanoNecesario = 1;
        if (altura > altura_nec || ancho > ancho_nec){
            final int mitad_altura = altura/2;
            final int mitad_ancho = ancho/2;
            while ((mitad_altura/inTamanoNecesario) > altura_nec && (mitad_ancho/inTamanoNecesario) > ancho_nec)
                inTamanoNecesario *= 2;
        }
        return inTamanoNecesario;
    }

    /**
     * comprobarDirectorio: verificamos que exista el directorio de la aplicacion en el almacenamiento externo, de no
     * ser asi intentamos crearlo
     */
    public static boolean comprobarDirectorio(Activity activity, File directorio){
        String estado_almacenamiento = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)) {      //Si hay un almacenamiento externo montado
            if (!directorio.exists()) {                                 //Si no existe el directorio de la app
                Log.i(TAG_UTIL, "No existe directorio, activity: " + activity.getLocalClassName());
                if (!directorio.mkdirs()) {                             //Si no se puede crear
                    Log.w(TAG_UTIL, "No se pudo crear un directorio, activity" + activity.getLocalClassName());
                    return false;
                } else return true;
            } else return true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(estado_almacenamiento)) {   //Si existe un almacenamiento pero
            toast(activity, activity.getString(R.string.almacenamiento_no_escritura));     //solo nos permite leer y no escribir
            Log.w(TAG_UTIL, "Solo es posible leer en el almacenamiento externo, activity" + activity.getLocalClassName());
            return false;
        }
        else {
            Log.w(TAG_UTIL, "No se puede leer ni escribir en el almacenamiento externo, activity: " + activity.getLocalClassName());
            toast(activity, activity.getString(R.string.almacenamiento_no_escritura_lectura));
            return false;
        }
    }

    /**
     * toast: crea un mensaje pequeno que se muestra unos segundos en pantalla (checar Toasts en Android)
     * @param activity: contexto de la aplicacion
     * @param mensaje: mensaje a mostrar en el toast
     */
    public static void toast(Activity activity, String mensaje){
        Toast.makeText(activity.getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    /**
     * crearBuilderDialogo: crea el contrsuctor de un dialogo para mostrarlo en pantalla posteriormente
     * @param activity: contexto de la app
     * @param titulo: titulo del dialogo
     * @param mensaje: mensaje del dialogo
     * @return constructor con las especificaciones dadas
     */
    public static AlertDialog.Builder crearBuilderDialogo(Activity activity, String titulo, String mensaje){
        AlertDialog.Builder dialogo = new AlertDialog.Builder(activity);
        dialogo.setTitle(titulo);
        dialogo.setCancelable(true);
        dialogo.setMessage(mensaje);
        dialogo.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return dialogo;
    }

    /**
     * setDirectorioApp: cambia la ruta del directorio de la app
     * @param directorioApp: nueva ruta en forma de string
     */
    public static void setDirectorioApp(String directorioApp) {
        Util.directorio_app = directorioApp;
    }

    /**
     * getDirectorioApp: regresa el directorio en el almacenamiento externo de la app
     * @return string con la ruta del directorio
     */
    public static String getDirectorioApp() {
        return directorio_app;
    }

    public static void configurarNuevoDispositivo(Activity activity) {
        ParticleDeviceSetupLibrary.startDeviceSetup(activity);
    }

    /**
     * clase ParticleAPI: Clase auxiliar para hacer lo relacionado con acceder a la nube de Particle
     */
    public static class ParticleAPI{

        static private ParticleCloud nube_particle;

        private static String email;
        private static String contrasena;

        private static ArrayList<String> ultimos_nombres_dispositivos = new ArrayList<>();
        private static ArrayList<String> ultimos_ids_dispositivos = new ArrayList<>();
        private static ArrayList<Boolean> ultimas_conexiones_dispositivos = new ArrayList<>();

        public static ParticleCloud obtenerNubeParticle(){
            return nube_particle;
        }

        public static void iniciarSesion(final IniciarSesionActivity activity, final String email, final String contrasena){
            //Ejecutamos otro hilo para iniciar sesion como nos indica la documentacion oficial en https://docs.particle.io/photon/android/
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {
                @Override
                public List<ParticleDevice> callApi(@NonNull ParticleCloud nueva_nube_particle) throws ParticleCloudException, IOException {
                    nube_particle = nueva_nube_particle;
                    nueva_nube_particle.logIn(email, contrasena);
                    ParticleAPI.email = email;
                    ParticleAPI.contrasena = contrasena;
                    return nueva_nube_particle.getDevices();
                }

                @Override
                public void onSuccess(List<ParticleDevice> ParticleDevices) {
                    Util.toast(activity, activity.getString(R.string.sesion_iniciada));

                    limpiarUltimaInformacionDispositivos();
                    ArrayList<String> conexion_dispositivos = new ArrayList<>();
                    for (ParticleDevice dispositivo : ParticleDevices) {                  //Obtenemos el id, nombre y estado de conexion de
                        ParticleAPI.ultimos_nombres_dispositivos.add(dispositivo.getName());            //cada dispositivo
                        ParticleAPI.ultimos_ids_dispositivos.add(dispositivo.getID());
                        ParticleAPI.ultimas_conexiones_dispositivos.add(dispositivo.isConnected());
                        conexion_dispositivos.add(dispositivo.isConnected() ? activity.getString(R.string.online) :
                                activity.getString(R.string.offline));
                    }

                    activity.iniciarSesionExito(Util.copiarArrayString(ultimos_nombres_dispositivos),
                            Util.copiarArrayString(ultimos_ids_dispositivos), conexion_dispositivos);
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    Util.toast(activity, activity.getString(R.string.sesion_no_iniciada));
                    activity.iniciarSesionFracaso();
                    Log.w(TAG_UTIL, "No se pudo iniciar sesion");
                }
            });
        }

        private static void limpiarUltimaInformacionDispositivos() {
            ParticleAPI.ultimas_conexiones_dispositivos.clear();
            ParticleAPI.ultimos_ids_dispositivos.clear();
            ParticleAPI.ultimos_nombres_dispositivos.clear();
        }

        public static void cerrarSesion() {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                @Override
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    nube_particle.logOut();
                    return null;
                }

                @Override
                public void onSuccess(Void v) {
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    Log.e(TAG_UTIL, "No se puede cerrar sesion de Particle");
                }
            });
        }

        public static void eliminarDispositivos(final Activity activity,
                                               final ArrayList<String> ids_dispositivos_seleccionados) {
            Util.toast(activity, activity.getString(R.string.actualizando_dispositivos));
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                @Override
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    ArrayList<ParticleDevice> dispositivos_eliminar = new ArrayList<>();
                    for (String id : ids_dispositivos_seleccionados)
                        dispositivos_eliminar.add(nube_particle.getDevice(id));
                    for (ParticleDevice dispositivo : dispositivos_eliminar) {
                        dispositivo.unclaim();
                    }
                    return null;
                }

                @Override
                public void onSuccess(Void avoid) {
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    Util.toast(activity, activity.getString(R.string.error_borrar_dispositivos));
                    Log.e(TAG_UTIL, "No se pudo eliminar el dispositivo: " + exception.toString());
                }
            });
        }

        public static void cambiarNombreDispositivo(final String id_dispositivo,
                                                    final String nombre_dispositivo_nuevo) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                @Override
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    nube_particle.getDevice(id_dispositivo).setName(nombre_dispositivo_nuevo);
                    return null;
                }

                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG_UTIL, "Nombre de dispositivo actualizado en la nube de Particle");
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    Log.e(TAG_UTIL, exception.toString());
                }
            });
        }

        public static void leerVariableEstadoModulo(final AdaptadorListaModulos adaptador,
                                                    final Integer numero_modulo, final String id_dispositivo) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Integer>() {
                @Override
                public Integer callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    Integer estado_modulo = ERROR;
                    try {
                        switch (numero_modulo) {
                            case 0:
                                estado_modulo = (Integer)nube_particle.getDevice(id_dispositivo).getVariable("estado_modulo_1");
                                break;
                            case 1:
                                estado_modulo = (Integer)nube_particle.getDevice(id_dispositivo).getVariable("estado_modulo_2");
                                break;
                            case 2:
                                estado_modulo = (Integer)nube_particle.getDevice(id_dispositivo).getVariable("estado_modulo_3");
                        }
                    }
                    catch (Exception e){
                        Log.e("SP AdapadosListaM", e.toString());
                    }
                    return estado_modulo;
                }

                @Override
                public void onSuccess(Integer estado) {
                    if (estado == ENCENDIDO)
                        adaptador.notificarEstadoEncendido(numero_modulo);
                    else if (estado == APAGADO)
                        adaptador.notificarEstadoApagado(numero_modulo);
                    else if (estado == ERROR){
                        adaptador.notificarEstadoError(numero_modulo);
                    }
                }

                @Override
                public void onFailure(ParticleCloudException exception) {
                    adaptador.notificarEstadoError(numero_modulo);
                    Log.e("SP AdapadosListaM", exception.toString());
                }
            });
        }

        public static void funcionApagarTodo(final Activity activity) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Integer>() {
                @Override
                public Integer callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    int resultado = 0;
                    int count = 0;
                    for (ParticleDevice dispositivo : nube_particle.getDevices()) {
                        try {
                            resultado += dispositivo.callFunction("apagar_todo");
                        } catch (Exception e) {
                            throw new ParticleCloudException(e);
                        }
                        count++;
                    }
                    if (resultado != count) return ERROR;
                    return APAGADO;
                }

                @Override
                public void onSuccess(Integer resultado) {
                    if (resultado == APAGADO) toast(activity, activity.getString(R.string.exito_apagar_todo));
                    else toast(activity, activity.getString(R.string.error_apagar_todo));
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    Log.e(TAG_DA, "No se pudo llamar la funcion apagar todo en un dispositivo:" + e.toString());
                    toast(activity, activity.getString(R.string.error_apagar_todo));
                }
            });
        }

        public static void agregarDispositivo(final Activity activity, final String id) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    nube_particle.claimDevice(id);
                    return null;
                }

                @Override
                public void onSuccess(Void aVoid) {
                    toast(activity, activity.getString(R.string.dispositivo_agregado));
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    Util.toast(activity, activity.getString(R.string.error_agregar_dispositivo));
                    Log.e(Util.TAG_DA, "No se puede agregar el dispositovo: " + e.getBestMessage());
                }
            });
        }

        public static void encenderModulo(final AdaptadorListaModulos adaptador, final String id, final int position, final Activity activity) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    ParticleDevice dispositivo = nube_particle.getDevice(id);
                    List<String> parametro = new ArrayList<>();
                    parametro.add(String.valueOf(position));
                    try {
                        for (String funcName : dispositivo.getFunctions()) {
                            Log.i(TAG_UTIL, "Device has function: " + funcName);
                        }

                        dispositivo.callFunction("encender_modulo", parametro);
                        Log.i(TAG_UTIL, "Se llamo la siguiente funcion: \"encender_modulo\", con un List<String> como parametro con" +
                                " un solo elemento: " + parametro.get(0));
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        throw new ParticleCloudException(e);
                    }
                    return null;
                }

                @Override
                public void onSuccess(Void aVoid) {
                    adaptador.notificarEstadoEncendido(position);
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    toast(activity, activity.getString(R.string.error_encender_modulo));
                    Log.e(Util.TAG_DA, "No se puede encender el modulo: " + e.getBestMessage());
                }
            });
        }

        public static void apagarModulo(final AdaptadorListaModulos adaptador, final String id, final int position, final Activity activity) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                public Void callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    ParticleDevice dispositivo = nube_particle.getDevice(id);
                    List<String> parametro = new ArrayList<>();
                    parametro.add(String.valueOf(position));
                    try {
                        dispositivo.callFunction("apagar_modulo", parametro);
                        Log.i(TAG_UTIL, "Se llamo la siguiente funcion: \"apagar_modulo\", con un List<String> como parametro con" +
                                " un solo elemento: " + parametro.get(0));
                    } catch (ParticleDevice.FunctionDoesNotExistException e) {
                        throw new ParticleCloudException(e);
                    }
                    return null;
                }

                @Override
                public void onSuccess(Void aVoid) {
                    adaptador.notificarEstadoApagado(position);
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    toast(activity, activity.getString(R.string.error_apagar_modulo));
                    Log.e(Util.TAG_DA, "No se puede apagar el modulo: " + e.getBestMessage());
                }
            });
        }

        public static ArrayList<String> getUltimosNombresDispositivos() {
            return ultimos_nombres_dispositivos;
        }

        public static ArrayList<String> getUltimosIdsDispositivos() {
            return ultimos_ids_dispositivos;
        }

        public static ArrayList<Boolean> getUltimasConexionesDispositivos() {
            return ultimas_conexiones_dispositivos;
        }

        public static void actualizarInformacionDispositivos(final HiloActualizarDispositivos hilo, final boolean interrupted,
                                                             final Semaphore semaforo) {
            Async.executeAsync(Util.ParticleAPI.obtenerNubeParticle(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {
                @Override
                public List<ParticleDevice> callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    List<ParticleDevice> dispositivos = null;
                    try {
                        semaforo.acquire();
                        if (!interrupted) dispositivos = nube_particle.getDevices();
                    } catch (InterruptedException e) {
                        Log.e(Util.TAG_HAD, "Error adquiriendo los dispositivos");
                    }
                    semaforo.release();
                    return dispositivos;
                }
                @Override
                public void onSuccess(List<ParticleDevice> dispositivos) {
                    limpiarUltimaInformacionDispositivos();
                    for (ParticleDevice dispositivo: dispositivos){
                        ParticleAPI.ultimos_nombres_dispositivos.add(dispositivo.getName());
                        ParticleAPI.ultimos_ids_dispositivos.add(dispositivo.getID());
                        ParticleAPI.ultimas_conexiones_dispositivos.add(dispositivo.isConnected());
                    }
                    hilo.actualizacionExitosa(Util.copiarArrayString(ultimos_nombres_dispositivos),
                            Util.copiarArrayString(ultimos_ids_dispositivos), Util.copiarArrayBoolean(ultimas_conexiones_dispositivos));
                }
                @Override
                public void onFailure(ParticleCloudException exception) {
                }
            });
        }

        public static void reiniciarSesion(final DispositivosActivity activity) {
            Async.executeAsync(nube_particle, new Async.ApiWork<ParticleCloud, Void>() {
                @Override
                public Void callApi(@NonNull ParticleCloud nueva_nube_particle) throws ParticleCloudException, IOException {
                    nueva_nube_particle.logIn(email, contrasena);
                    return null;
                }

                @Override
                public void onSuccess(Void aVoid) {
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    Util.toast(activity, activity.getString(R.string.sesion_no_iniciada));
                    activity.reiniciarSesionFracaso();
                    Log.w(TAG_UTIL, "No se pudo reiniciar sesion");
                }
            });
        }
    }

    private static ArrayList<String> copiarArrayString(ArrayList<String> arreglo) {
        ArrayList<String> copia = new ArrayList<>();
        for (int i = 0; i < arreglo.size(); i++) copia.add(arreglo.get(i));
        return copia;
    }

    private static ArrayList<Boolean> copiarArrayBoolean(ArrayList<Boolean> arreglo) {
        ArrayList<Boolean> copia = new ArrayList<>();
        for (int i = 0; i < arreglo.size(); i++) copia.add(arreglo.get(i));
        return copia;
    }
}
