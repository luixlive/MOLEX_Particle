package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;

import static android.text.InputType.TYPE_CLASS_NUMBER;

/**
 * Clase DispositivosActivity: muestra la lista con los dispositivos Photon asociados a la cuenta que esta iniciada y muestra
 * sus estados de conexion
 * Created by LUIS ALFONSO on 17/12/2015.
 */
public class DispositivosActivity extends AppCompatActivity {

    private ArrayList<String> nombres_dispositivos; //Informacion de los dispositivos y del almacenamiento en el smartphone del usuario
    private ArrayList<String> ids_dispositivos;
    private ArrayList<Boolean> conexion_dispositivos;
    private boolean almacenamiento_avatares_posible = false;
    private File directorio_avatares;
    private File directorio_app;
    private int numero_dispositivos;

    private volatile AdaptadorListaDispositivos adaptador;
    private boolean dispositivos_seleccionados[];
    private boolean dispositivos_conectados_filtrados = false;

    private static HiloActualizarDispositivos hilo_actualizar_dispositivos;  //Runnable que correra sobre otro hilo para actualizar la conexion

    private int ancho_avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos);

        capturarInformacionSesion();
        calcularAnchoAvatares();
        recuperarInformacionDelBundle(savedInstanceState);
        crearListaDispositivos();
        iniciarEventoBotonHome();
        iniciarHiloActualizacionConexiones();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.app_menu, menu);       //Creamos el menu superior
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Si el usuario pulsa un item del menu, realizamos la accion correspondiente
        switch (item.getItemId()) {
            case R.id.iFiltrar_dispositivos:
                if (dispositivos_conectados_filtrados = adaptador.filtrarDispositivosPresionado())
                    item.setIcon(R.mipmap.icono_quitar_filtrado);
                else item.setIcon(R.mipmap.icono_filtrar);
                return true;
            case R.id.iSetup_dispositivo:
                ParticleDeviceSetupLibrary.startDeviceSetup(this);
                return true;
            case R.id.iAgregar_dispositivos:
                dialogoAgregarDispositivo();
                return true;
            case R.id.iCerrar_sesion:
                crearDialogoCerrarSesion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state){  //En caso de reiniciar la actividad (al girar la orientacion del celular
        //por ejemplo) se guardan los dispositivos que se tenian seleccionados.
        state.putBooleanArray("DispositivosSeleccionado", dispositivos_seleccionados);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        if(isFinishing()) {
            hilo_actualizar_dispositivos.interrumpir();
            Util.ParticleAPI.cerrarSesion();
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
            conexion_dispositivos.add(conexion.equals(getString(R.string.online)) ? Util.CONECTADO: Util.DESCONECTADO);
    }

    /**
     * calcularAnchoAvatares: calcula el ancho adecuado de los avatares dependiendo del dispositivo smartphone que se utilice
     */
    private void calcularAnchoAvatares() {
        DisplayMetrics metrics = new DisplayMetrics();                  //Obtenemos las medidas de la pantalla del celular
        getWindowManager().getDefaultDisplay().getMetrics(metrics);     //para ajustar lo ancho de las imagenes
        ancho_avatar = (int)(metrics.widthPixels * Util.RELACION_WIDTH_AVATARES);
    }

    /**
     * recuperarInformacionDelBundle: recuperamos la informacion importante que el usuario tenia en pantalla antes
     * de que la activity se pausara
     * @param estado: bundle del estado de la activity que se quiere recuperar (dispositivos que habia seleccionado)
     */
    private void recuperarInformacionDelBundle(Bundle estado) {
        if (estado != null){
            dispositivos_seleccionados = estado.getBooleanArray("DispositivosSeleccionado");
        } else{
            dispositivos_seleccionados = new boolean[numero_dispositivos];
            for (int index = 0; index < numero_dispositivos; index++)
                dispositivos_seleccionados[index] = false;
        }
    }

    /**
     * crearListaDispositivos: crea las vistas de la lista de dispositivos de la sesion iniciada con la informacion capturada del
     * servidor de Particle y crea un evento para iniciar la activity DispositivoSeleccionadoActivity cuando pulsen un dispositivo
     */
    private void crearListaDispositivos() {
        GridView dispositivos = (GridView) findViewById(R.id.gvDispositivos);
        adaptador = new AdaptadorListaDispositivos(DispositivosActivity.this, nombres_dispositivos, conexion_dispositivos,
                obtenerAvatares());
        dispositivos.setAdapter(adaptador);

        dispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {     //Si pulsan algun dispositivo en la lista
            @Override
            //(mientras que no sea el avatar)
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {    //Si se pulsa un dispositivo
                adaptador.sombrear(true, position);
                Util.vibrar(DispositivosActivity.this);

                int posicion_real = obtenerPosicionRealDispositivo(position);
                iniciarActivityDispositivoSeleccionado(posicion_real);
            }
        });

        crearContextMenu(dispositivos);
    }

    /**
     * crearContextMenu: crea el menu que apaerce en la parte superior de la app al mantener pulsado un item de la lista de
     * dispositivos, este menu permite eliminar uno o varios dispositivos a la vez
     * @param dispositivos: ListView de la lista a la que se le agrega el Context Menu
     */
    private void crearContextMenu(final GridView dispositivos) {
        dispositivos.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);        //Si mantienen pulsado un dispositivo, aparece la opcion de
        dispositivos.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {     //elegir mas y eliminarlos

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                dispositivos_seleccionados[position] = checked;
                adaptador.sombrear(checked, position);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                for (int index = 0; index < nombres_dispositivos.size(); index++)
                    adaptador.sombrear(dispositivos_seleccionados[index], index);

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
                switch (item.getItemId()) {
                    case R.id.borrar:
                        ArrayList<String> ids_dispositivos_seleccionados = new ArrayList<>();
                        for (int index = 0; index < numero_dispositivos; index++) {
                            if (dispositivos_seleccionados[index]) {
                                ids_dispositivos_seleccionados.add(ids_dispositivos.get(index));
                            }
                        }
                        Util.ParticleAPI.eliminarDispositivos(DispositivosActivity.this, ids_dispositivos_seleccionados);
                        break;
                    case android.R.id.home:
                        for (int index = 0; index < numero_dispositivos; index++)
                            dispositivos_seleccionados[index] = false;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adaptador.sombrearTodos(false);
            }
        });
    }

    /**
     * iniciarEventoBotonHome: crea un evento con las acciones a realizar cuando se pulse el boton home
     */
    private void iniciarEventoBotonHome() {
        final ImageButton boton_home = (ImageButton) findViewById(R.id.ibHome);

        boton_home.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn_presionado));
                    Util.vibrar(DispositivosActivity.this);
                }
                if (event.getX() < 0 || event.getY() < 0 || event.getX() > boton_home.getWidth() || event.getY() > boton_home.getHeight()) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                    Util.ParticleAPI.funcionApagarTodo(DispositivosActivity.this);
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
            Log.i(Util.TAG_DA, "Se inicio el hilo para actualizar la conexion");    //por lo que si no es nulo, no lo volvemos a iniciar
        }
        else hilo_actualizar_dispositivos.setActivity(this);                    //Solamente acualizamos la activity nueva en la clase del hilo
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Util.REQUEST_CODE_DISPOSITIVO_SELECCIONADO && resultCode == Activity.RESULT_OK) {
            adaptador.setAvatares(obtenerAvatares());       //Si el usuario cambio su avatar en la activity DispositivoSeleccinadoA
            adaptador.notifyDataSetChanged();               //lo actualizamos aqui tambien
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * crearDialogoCerrarSesion: muestra un dialogo al usuario que le pregunta si desea cerrar sesion
     */
    private void crearDialogoCerrarSesion() {
        AlertDialog.Builder dialogo_cerrar_sesion = Util.crearBuilderDialogo(this,  //para cerrar su sesion en Particle
                getString(R.string.dialogo_cerrar_sesion), getString(R.string.mensaje_dialogo_cerrar_sesion));
        dialogo_cerrar_sesion.setPositiveButton(R.string.dialogo_cerrar_sesion_si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), IniciarSesionActivity.class);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog alert_dialogo = dialogo_cerrar_sesion.create();
        alert_dialogo.show();
    }

    /**
     * dialogoAgregarDispositivo: muestra al usuario un dialogo que le permite indicar el id de un nuevo dispositivo
     * que quiera agregar a su lista
     */
    private void dialogoAgregarDispositivo() {
        AlertDialog.Builder dialogo_agregar_dispositivo = Util.crearBuilderDialogo(this,
                getString(R.string.dialogo_agregar_dispositivo), getString(R.string.mensaje_dialogo_agregar_dispositivo));

        //Creamos el cuadro de texto donde el usuario pondra el id del nuevo dispositivo
        final EditText cuadro_texto = new EditText(this);
        cuadro_texto.setSingleLine();
        cuadro_texto.setInputType(TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        cuadro_texto.setLayoutParams(parametros);
        dialogo_agregar_dispositivo.setView(cuadro_texto);

        dialogo_agregar_dispositivo.setPositiveButton(R.string.dialogo_agregar_dispositivo_si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nuevo_id = cuadro_texto.getText().toString();
                Util.ParticleAPI.agregarDispositivo(DispositivosActivity.this, nuevo_id);
            }
        });

        AlertDialog alert_dialogo = dialogo_agregar_dispositivo.create();
        alert_dialogo.show();
    }

    /**
     * obtenerAvatares: funcion que extrae los avatares que el usuario habia preestablecido la ultima vez que los modifico
     * @return arreglo que contiene los avatares en el mismo orden que los dispositivos
     */
    private Bitmap[] obtenerAvatares(){
        Bitmap avatares_dispositivos[] = new Bitmap[numero_dispositivos];
        if (almacenamiento_avatares_posible){
            String images[] = directorio_avatares.list();      //Obtenemos los archivos dentro del directorio de avatares

            int count = 0;
            for (String id_dispositivo: ids_dispositivos){
                if (Arrays.asList(images).contains(id_dispositivo + getString(R.string.tipo_imagen))){
                    avatares_dispositivos[count++] = Util.cortarImagenCircuilar(Util.obtenerImagenReducida(directorio_avatares +
                            File.separator + id_dispositivo + getString(R.string.tipo_imagen), ancho_avatar, ancho_avatar));
                } else avatares_dispositivos[count++] = Util.cortarImagenCircuilar(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.photon));
            }
        }
        else{
            for (int index = 0; index < numero_dispositivos; index++)
                avatares_dispositivos[index] = null;
        }
        return avatares_dispositivos;
    }

    /**
     * actualizarDispositivos: funcion que se llama desde una clase externa para actualizar el estado de conexion y nombre de
     * los dispositivos en la interfaz grafica, tanto en esta activity como en la activity DispositivoSeleccionadoActivity
     * @param conexiones: lista de tamano igual al numero de dispositivos del usuario con los nuevos estados de conexion actualizados
     */
    public void actualizarDispositivos(ArrayList<String> nombres, ArrayList<Boolean> conexiones){
        adaptador.setNombresDispositivos(nombres);
        adaptador.setConexionesDispositivos(conexiones);
        adaptador.notifyDataSetChanged();

        for (int index = 0; index < numero_dispositivos; index++) {         //Actualizamos los ArrayList
            conexion_dispositivos.set(index, conexiones.get(index));
            nombres_dispositivos.set(index, nombres.get(index));
        }

        if (DispositivoSeleccionadoActivity.getIndexActual() != null)       //Actualizamos el dispositivo que se esta mostrando en
            DispositivoSeleccionadoActivity.actualizarDispositivoActual(    //la activity DispositivoSeleccionado si es que existe uno
                    conexion_dispositivos.get(DispositivoSeleccionadoActivity.getIndexActual()),
                    nombres_dispositivos.get(DispositivoSeleccionadoActivity.getIndexActual()) ,this);
        Log.v("Actualizacion Conexion", "Se han actualizado los estados de conexion de dispositivos en la UI");
    }

    /**
     * obtenerPosicionRealDispositivo: regresa la posicion del dispositivo pulsado por el usuario en la lista total de dispositivos,
     * esto ya que al filtrar los dispositivos conectados, no tenemos la posicion real en la lista, si no la posicion en la lista
     * filtrada, esta funcion regresa el valor real ya sea si estan filtrados o no
     * @param posicion_lista_actual: posicion en la lista filtrada o sin filtrar
     * @return posicion en la lista sin filtrar (posicion real)
     */
    private int obtenerPosicionRealDispositivo(int posicion_lista_actual) {
        int posicion_sin_filtrar = posicion_lista_actual;
        if (dispositivos_conectados_filtrados) {
            posicion_sin_filtrar = 0;
            int conteo_regresivo = posicion_lista_actual;
            for (boolean conectado : dispositivos_seleccionados) {
                if (conectado)
                    conteo_regresivo--;
                if (conteo_regresivo == -1)
                    break;
                posicion_sin_filtrar++;
            }
        }
        return posicion_sin_filtrar;
    }

    /**
     * iniciarActivityDispositivoSeleccionado: inicia la activity DispositivoSeleccionadoActivity con el dispositivo pulsado
     * por el usuario
     * @param posicion_real: posicion del dispositivo en la lista real de dispositivos (no la filtrada, si es que se filtro)
     */
    private void iniciarActivityDispositivoSeleccionado(final int posicion_real) {
        Intent intent = new Intent(getApplicationContext(), DispositivoSeleccionadoActivity.class);
        intent.putExtra("nombre_dispositivo", nombres_dispositivos.get(posicion_real));
        intent.putExtra("id_dispositivo", ids_dispositivos.get(posicion_real));
        intent.putExtra("conexion_dispositivo", conexion_dispositivos.get(posicion_real));
        intent.putExtra("directorio_app", directorio_app.toString());
        intent.putExtra("directorio_avatares", directorio_avatares.toString());
        intent.putExtra("almacenamiento_avatares_posible", almacenamiento_avatares_posible);
        intent.putExtra("index_dispositivo", posicion_real);
        startActivityForResult(intent, Util.REQUEST_CODE_DISPOSITIVO_SELECCIONADO); //Iniciamos la activity
        Log.i(Util.TAG_DA, "Se pulso al dispositivo numero: " + posicion_real + ". Iniciando activity DispositivoSeleccionadoActivity");
        new AsyncTask<Void, Void, Void>(){          //Corremos un segundo hilo que cuente medio segundo para quitar la seleccion
            @Override                               //del dispositivo pulsado (al pulsarlo se selecciono y se pinto el fondo de gris)
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid){
                adaptador.sombrear(false, posicion_real);   //Quitamos el sombreado gris
            }
        }.execute();
    }
}