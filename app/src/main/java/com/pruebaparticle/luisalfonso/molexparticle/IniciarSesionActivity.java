/**
 * Autor: Luis Alfonso Chávez Abbadie
 * 20/12/2015
 * Ultima edicion 20/01/2016
 * Proyecto SmartPower
 */
package com.pruebaparticle.luisalfonso.molexparticle;

import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;

/**
 * Clase IniciarSesionActivity: activity main de la app, donde el usuario inicia sesion en ParticleCloud
 * @author Luis Alfonso Chávez Abbadie
 */
public class IniciarSesionActivity extends AppCompatActivity {

    private File directorio_app;         //ruta del directorio de la app en el almacenamiento externo

    private EditText et_contrasena;      //Edit Text donde se escribe la contrasena

    private String email;                //Correo que el usuario escribe en el Edit Text del email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepararDirectorioApp();
        prepararEditTexts();
        iniciarSDKsParticle();
    }

    /**
     * prepararDirectorioApp: obtiene la ruta a la carpeta de la app y analiza que exista, si no es asi la crea
     */
    private void prepararDirectorioApp() {
        directorio_app = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + getString(R.string.app_name));
        if (!Util.comprobarDirectorio(this, directorio_app))
            Util.toast(this, getString(R.string.almacenamiento_no_escritura));
    }

    /**
     * prepararEditTexts: Obtiene las vistas de los cuadros donde se escribiran el correo y la contrasena y los prepara
     */
    private void prepararEditTexts() {
        AutoCompleteTextView et_email = (AutoCompleteTextView) findViewById(R.id.etEmail);
        et_contrasena = (EditText) findViewById(R.id.etPass);

        ponerListaUsuariosRegistrados(et_email);
    }

    /**
     * iniciarSDKsParticle: Se inician los sdks como se indica en la documentacion oficial
     */
    private void iniciarSDKsParticle() {
        ParticleCloudSDK.init(getApplicationContext());
        ParticleDeviceSetupLibrary.init(getApplicationContext(), DispositivosActivity.class);
    }

    /**
     * iniciarSesion: funcion que se llama cuando el usuario pulsa el boton de inicio de sesion, accede al servidor
     * de ParticleCloud e intenta iniciar la sesion con los valores de email y contraseña que se hayan escrito
     * @param boton: objeto que representa la vista del boton de inicio de sesion
     */
    public void iniciarSesion(View boton){
        Util.vibrar(this);
        Log.i(Util.TAG_ISA, "Intento de inicio de sesion");

        botonIniciarSesionPulsado(boton);

        email = obtenerCorreoEscrito();
        String pass = obtenerContrasenaEscrita();
        Util.ParticleAPI.iniciarSesion(this, email, pass);
    }

    /**
     * botonIniciarSesionPulsado: cambia el color del texto y del fondo del boton para aparentar un boton animado
     * @param boton: vista del boton de iniciar sesion
     */
    private void botonIniciarSesionPulsado(View boton) {
        Button boton_iniciar_sesion = (Button)boton;
        boton_iniciar_sesion.setBackgroundColor(ContextCompat.getColor(this, R.color.blanco));
        boton_iniciar_sesion.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        boton_iniciar_sesion.setClickable(false);
        esconder_teclado();
    }

    /**
     * esconder_teclado: esconde el teclado de la pantalla cuando pulsa iniciar sesion
     */
    public void esconder_teclado() {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if(view == null) view = new View(this);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * obtenerContrasenaEscrita: obtiene la contrasena que el usuario ingreso al EditText de la contrasena
     * @return string con la contrasena
     */
    private String obtenerContrasenaEscrita() {
        EditText et_pass = (EditText)findViewById(R.id.etPass);
        return et_pass.getText().toString();
    }

    /**
     * obtenerCorreoEscrito: obtiene el correo que el usuario ingreso al EditText del correo
     * @return string con el correo electronico
     */
    private String obtenerCorreoEscrito() {
        EditText et_email = (EditText)findViewById(R.id.etEmail);
        return et_email.getText().toString();
    }

    /**
     * ponerListaUsuariosRegistrados: crea una lista con los usuarios que han iniciado sesion antes y la pone al
     * EditText del correo para que al ir escribiendo aparezcan los usuarios como opcion
     * @param et_email: la vista del EditText del correo
     */
    private void ponerListaUsuariosRegistrados(AutoCompleteTextView et_email) {
        String[] usuarios_registrados = directorio_app.list();
        ArrayAdapter<String> adaptador_autocompletar = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                usuarios_registrados);
        et_email.setAdapter(adaptador_autocompletar);
        et_email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    et_contrasena.callOnClick();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * iniciarSesionExito: Realiza las acciones correspondientes de haber iniciado sesion correctamente
     * (abrir la activity DispositivosActivity)
     * @param nombres_dispositivos: nombres de los dispositivos que tiene este usuario
     * @param ids_dispositivos: ids de dichos dispositivos
     * @param conexion_dispositivos: su estado de conexion
     */
    public void iniciarSesionExito(ArrayList<String> nombres_dispositivos, ArrayList<String> ids_dispositivos,
                                                ArrayList<String> conexion_dispositivos) {
        Util.setDirectorioApp(directorio_app.toString() + File.separator + email);
        Intent intent = new Intent(getApplicationContext(), DispositivosActivity.class);    //Enviamos la informacion obtenida
        intent.putStringArrayListExtra("nombres_dispositivos", nombres_dispositivos);       //a la activity DispositivosActivity
        intent.putStringArrayListExtra("ids_dispositivos", ids_dispositivos);
        intent.putStringArrayListExtra("conexion_dispositivos", conexion_dispositivos);
        finish();
        Log.i(Util.TAG_ISA, "Inicio de sesion exitoso, cambio de Activity a DispositivosActivity");
        startActivity(intent);
    }

    /**
     * iniciarSesionFracaso: Realiza las acciones correspondientes de haber fracasdo al intentr iniciar sesion
     * (vuelve a poner el boton en su estado original)
     */
    public void iniciarSesionFracaso() {
        Button boton_iniciar_sesion = (Button)findViewById(R.id.btIniciarSesion);
        boton_iniciar_sesion.setBackgroundColor(ContextCompat.getColor(IniciarSesionActivity.this, R.color.colorPrimary));
        boton_iniciar_sesion.setTextColor(ContextCompat.getColor(IniciarSesionActivity.this, R.color.blanco));
        boton_iniciar_sesion.setClickable(true);
    }
}