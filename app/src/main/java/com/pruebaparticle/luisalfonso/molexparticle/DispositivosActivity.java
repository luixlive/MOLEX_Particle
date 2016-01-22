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
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static android.text.InputType.TYPE_CLASS_TEXT;

/**
 * Clase DispositivosActivity: muestra la lista con los dispositivos Photon asociados a la cuenta que esta iniciada y muestra
 * sus estados de conexion
 * Created by LUIS ALFONSO on 17/12/2015.
 */
public class DispositivosActivity extends AppCompatActivity {

    private ArrayList<String> nombres_dispositivos = new ArrayList<>(); //Informacion de los dispositivos y del almacenamiento en el smartphone del usuario
    private ArrayList<String> ids_dispositivos = new ArrayList<>();
    private ArrayList<Boolean> conexion_dispositivos = new ArrayList<>();
    private boolean almacenamiento_avatares_posible = false;
    private File directorio_avatares;
    private int numero_dispositivos;

    private volatile AdaptadorListaDispositivos adaptador;
    private boolean dispositivos_conectados_filtrados = false;

    private static HiloActualizarDispositivos hilo_actualizar_dispositivos;  //Runnable que correra sobre otro hilo para actualizar la conexion

    private int ancho_avatar;

    private static boolean bandera_configurando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos);

        capturarInformacionSesion();
        calcularAnchoAvatares();
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
    public boolean onOptionsItemSelected(MenuItem item) {   //Se llama cuando se pulsa un item del menu
        switch (item.getItemId()) {
            case R.id.iFiltrar_dispositivos:
                cambiarEstadoFiltrado(item);
                return true;
            case R.id.iSetup_dispositivo:
                crearDialogoConfigurarDispositivo();
                return true;
            case R.id.iAgregar_dispositivos:
                crearDialogoAgregarDispositivo();
                return true;
            case R.id.iCerrar_sesion:
                crearDialogoCerrarSesion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()) {
            hilo_actualizar_dispositivos.interrumpir();
            hilo_actualizar_dispositivos = null;
            Util.ParticleAPI.cerrarSesion();
        }
    }

    /**
     * capturarInformacionSesion: si se reinicia la activity despues de la configuracion de un dispositivo recupera las vistas
     * de la ultima actualizacion, si se crea por primera vez se recibe el intent de la activity de inicio de sesion
     */
    private void capturarInformacionSesion() {
        if (bandera_configurando) {
            nombres_dispositivos = Util.ParticleAPI.getUltimosNombresDispositivos();
            ids_dispositivos = Util.ParticleAPI.getUltimosIdsDispositivos();
            conexion_dispositivos = Util.ParticleAPI.getUltimasConexionesDispositivos();
            Util.ParticleAPI.reiniciarSesion(this);
            bandera_configurando = false;
        } else{
            Intent intent = getIntent();
            nombres_dispositivos = intent.getStringArrayListExtra("nombres_dispositivos");
            ids_dispositivos = intent.getStringArrayListExtra("ids_dispositivos");
            ArrayList<String> conexion_dispositivos_string = intent.getStringArrayListExtra("conexion_dispositivos");
            if (conexion_dispositivos_string != null) {
                for (String conexion : conexion_dispositivos_string)
                    conexion_dispositivos.add(conexion.equals(getString(R.string.online)) ? Util.CONECTADO : Util.DESCONECTADO);
            }
        }
        directorio_avatares = new File(Util.getDirectorioApp() + File.separator + getString(R.string.directorio_avatares));
        almacenamiento_avatares_posible = Util.comprobarDirectorio(this, directorio_avatares);
        numero_dispositivos = nombres_dispositivos.size();
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
     * crearListaDispositivos: crea las vistas de la lista de dispositivos de la sesion iniciada con la informacion capturada del
     * servidor de Particle y crea un evento para iniciar la activity DispositivoSeleccionadoActivity cuando pulsen un dispositivo
     */
    private void crearListaDispositivos() {
        adaptador = new AdaptadorListaDispositivos(DispositivosActivity.this, nombres_dispositivos, conexion_dispositivos,
                obtenerAvatares());

        ListView dispositivos = (ListView) findViewById(R.id.lvDispositivos);
        dispositivos.setAdapter(adaptador);
        dispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {     //Si pulsan algun dispositivo en la lista
            @Override
            //(mientras que no sea el avatar)
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Util.vibrar(DispositivosActivity.this);

                int posicion_real = obtenerPosicionRealDispositivo(position);
                adaptador.sombrear(true, posicion_real);
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
    private void crearContextMenu(final ListView dispositivos) {
        dispositivos.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);        //Si mantienen pulsado un dispositivo, aparece la opcion de
        dispositivos.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {     //elegir mas y eliminarlos

            private ArrayList<Boolean> dispositivos_seleccionados;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int posicion = obtenerPosicionRealDispositivo(position);
                dispositivos_seleccionados.set(posicion, checked);
                adaptador.sombrear(checked, posicion);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater creador_menu = mode.getMenuInflater();
                creador_menu.inflate(R.menu.menu_context, menu);
                dispositivos_seleccionados = new ArrayList<>();
                for (int index = 0; index < numero_dispositivos; index++)
                    dispositivos_seleccionados.add(false);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                final ArrayList<String> ids_dispositivos_seleccionados = new ArrayList<>();
                switch (item.getItemId()) {
                    case R.id.borrar:
                        ids_dispositivos_seleccionados.clear();
                        for (int index = 0; index < numero_dispositivos; index++) {
                            if (dispositivos_seleccionados.get(index)) {
                                ids_dispositivos_seleccionados.add(ids_dispositivos.get(index));
                            }
                        }
                        AlertDialog.Builder dialogo_eliminar = Util.crearBuilderDialogo(DispositivosActivity.this,
                                DispositivosActivity.this.getString(R.string.dialogo_eliminar),
                                DispositivosActivity.this.getString(R.string.mensaje_dialogo_eliminar));
                        dialogo_eliminar.setPositiveButton(DispositivosActivity.this.getString(R.string.dialogo_eliminar_si),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Util.ParticleAPI.eliminarDispositivos(DispositivosActivity.this, ids_dispositivos_seleccionados);
                                        mode.finish();
                                    }
                                });
                        dialogo_eliminar.create().show();
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
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                    Util.ParticleAPI.funcionApagarTodo(DispositivosActivity.this);
                }
                if (event.getX() < 0 || event.getY() < 0 || event.getX() > boton_home.getWidth() || event.getY() > boton_home.getHeight())
                    ((ImageButton) v).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.casa_btn));
                return true;
            }
        });
    }

    /**
     * iniciarHiloActualizacionConexiones: inicia un hilo en segundo plano que actualice los estados de conexion cada 5 segundos
     * y lo mantiene activo y actualizado siempre que se destruya la activity a menos que el usuario se salga de la app
     */
    private void iniciarHiloActualizacionConexiones() {
        if (hilo_actualizar_dispositivos == null) {
            hilo_actualizar_dispositivos = new HiloActualizarDispositivos(this);
            new Thread(hilo_actualizar_dispositivos).start();
            Log.i(Util.TAG_DA, "Se inicio el hilo para actualizar la conexion");
        }
        else hilo_actualizar_dispositivos.setActivity(this);                    //Solamente acualizamos la activity nueva en la clase del hilo
    }

    /**
     * crearDialogoConfigurarDispositivo: se llama al pulsar el item configurar dispositivo del menu y muestra un dialog permitiendo
     * al usuario decidir si configurar un nuevo dispositivo o no, aprovecha el ParticleDeviceSetupLibrary
     */
    private void crearDialogoConfigurarDispositivo() {
        AlertDialog.Builder dialogo_configurar = Util.crearBuilderDialogo(this, getString(R.string.dialogo_configurar_dispositivo),
                getString(R.string.mensaje_dialogo_configurar_d));
        dialogo_configurar.setPositiveButton(getString(R.string.dialogo_configurar_si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bandera_configurando = true;
                Util.configurarNuevoDispositivo(DispositivosActivity.this);
            }
        });
        (dialogo_configurar.create()).show();
    }

    /**
     * cambiarEstadoFiltrado: se llama cuando se pulsa el boton filtrar y cambia el estado de filtrado
     * @param item: item filtrar dispositivos del menu
     */
    private void cambiarEstadoFiltrado(MenuItem item) {
        dispositivos_conectados_filtrados = adaptador.filtrarDispositivosPresionado();
        if (dispositivos_conectados_filtrados) item.setIcon(R.mipmap.icono_quitar_filtrado);
        else item.setIcon(R.mipmap.icono_filtrar);
    }

    /**
     * dialogoAgregarDispositivo: muestra al usuario un dialogo que le permite indicar el id de un nuevo dispositivo
     * que quiera agregar a su lista
     */
    private void crearDialogoAgregarDispositivo() {
        AlertDialog.Builder dialogo_agregar_dispositivo = Util.crearBuilderDialogo(this,
                getString(R.string.dialogo_agregar_dispositivo), getString(R.string.mensaje_dialogo_agregar_dispositivo));
        //Creamos el cuadro de texto donde el usuario pondra el id del nuevo dispositivo
        final EditText cuadro_texto = new EditText(this);
        cuadro_texto.setSingleLine();
        cuadro_texto.setInputType(TYPE_CLASS_TEXT);
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
        dialogo_agregar_dispositivo.create().show();
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
        dialogo_cerrar_sesion.create().show();
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
    public void actualizarDispositivos(ArrayList<String> nombres, ArrayList<Boolean> conexiones, ArrayList<String> ids){
        numero_dispositivos = nombres.size();

        conexion_dispositivos.clear();
        nombres_dispositivos.clear();
        ids_dispositivos.clear();

        for (int index = 0; index < numero_dispositivos; index++) {         //Actualizamos los ArrayList
            conexion_dispositivos.add(conexiones.get(index));
            nombres_dispositivos.add(nombres.get(index));
            ids_dispositivos.add(ids.get(index));
        }

        adaptador.setNombresDispositivos(nombres);
        adaptador.setConexionesDispositivos(conexiones);
        adaptador.setAvatares(obtenerAvatares());
        adaptador.notifyDataSetChanged();

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
            for (boolean conectado : conexion_dispositivos) {
                if (conectado)
                    if (--conteo_regresivo == -1)
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
    private void iniciarActivityDispositivoSeleccionado(int posicion_real) {
        Intent intent = new Intent(getApplicationContext(), DispositivoSeleccionadoActivity.class);
        intent.putExtra("nombre_dispositivo", nombres_dispositivos.get(posicion_real));
        intent.putExtra("id_dispositivo", ids_dispositivos.get(posicion_real));
        intent.putExtra("conexion_dispositivo", conexion_dispositivos.get(posicion_real));
        intent.putExtra("directorio_avatares", directorio_avatares.toString());
        intent.putExtra("almacenamiento_avatares_posible", almacenamiento_avatares_posible);
        intent.putExtra("index_dispositivo", posicion_real);
        startActivityForResult(intent, Util.REQUEST_CODE_DISPOSITIVO_SELECCIONADO); //Iniciamos la activity
        Log.i(Util.TAG_DA, "Se pulso al dispositivo numero: " + posicion_real + ". Iniciando activity DispositivoSeleccionadoActivity");
        quitarSombreadoDispositivo(posicion_real);
    }

    /**
     * quitarSombreadoDispositivo: al pulsar un dispositivo se sombrea su fondo, se llama esta funcion para que se cuente medio
     * segundo en otro hilo y al terminar vuelva a colorear el fondo como era originalmente
     * @param posicion_real: posicion del dispositivo en la lista completa de dispositivos
     */
    private void quitarSombreadoDispositivo(final int posicion_real) {
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

    /**
     * reiniciarSesionFracaso: si al intentar reiniciar la sesion del usuario hay un error, se regresa a la activity IniciarSesionActivity
     * (La sesion se reinicia cuando se configura un nuevo dispositivo, ya que el sdk de particle termina con las activities anteriores)
     */
    public void reiniciarSesionFracaso() {
        Intent intent = new Intent(getApplicationContext(), IniciarSesionActivity.class);
        finish();
        startActivity(intent);
    }
}