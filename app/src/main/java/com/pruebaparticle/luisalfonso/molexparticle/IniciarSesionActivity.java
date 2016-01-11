/**
 * Autor: Luis Alfonso Chávez Abbadie
 * 20/12/2015
 * Proyecto SmartPower, MOLEX
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;

/**
 * Clase IniciarSesionActivity: activity main de la app, donde el usuario inicia sesion en ParticleCloud
 * @author Luis Alfonso Chávez Abbadie
 */
public class IniciarSesionActivity extends AppCompatActivity {

    private static final String Tag = "SP IniciarSesionA";

    private File directorio_app;                //ruta del directorio de la app en el almacenamiento externo

    private EditText et_contrasena;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepararDirectorioApp();
        prepararEditTexts();
    }

    /**
     * iniciarSesion: funcion que se llama cuando el usuario pulsa el boton de inicio de sesion, accede al servidor
     * de ParticleCloud e intenta iniciar la sesion con los valores de email y contraseña que se hayan escrito
     * @param boton: objeto que representa la vista del boton de inicio de sesion
     */
    public void iniciarSesion(final View boton){
        Log.i(Tag, "Intento de inicio de sesion");
        final Button boton_iniciar_sesion = (Button)boton;
        boton_iniciar_sesion.setBackgroundColor(ContextCompat.getColor(this, R.color.blanco));
        boton_iniciar_sesion.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        boton_iniciar_sesion.setClickable(false);

        EditText et_email = (EditText)findViewById(R.id.etEmail);       //Obtenemos los strings del correo y contraseña
        EditText et_pass = (EditText)findViewById(R.id.etPass);         //escritos por el usuario
        final String email = et_email.getText().toString();
        final String pass = et_pass.getText().toString();

        //Ejecutamos otro hilo para iniciar sesion como nos indica la documentacion oficial en https://docs.particle.io/photon/android/
        Async.executeAsync(SparkCloud.get(getApplicationContext()), new Async.ApiWork<SparkCloud, List<SparkDevice>>() {
            @Override
            public List<SparkDevice> callApi(SparkCloud sparkCloud) throws SparkCloudException, IOException {
                sparkCloud.logIn(email, pass);
                return sparkCloud.getDevices();
            }

            @Override
            public void onSuccess(List<SparkDevice> sparkDevices) {
                Util.toast(IniciarSesionActivity.this, getString(R.string.sesion_iniciada));
                ArrayList<String> nombres_dispositivos = new ArrayList<>();
                ArrayList<String> ids_dispositivos = new ArrayList<>();
                ArrayList<String> conexion_dispositivos = new ArrayList<>();
                for (SparkDevice dispositivo : sparkDevices) {                  //Obtenemos el id, nombre y estado de conexion de
                    nombres_dispositivos.add(dispositivo.getName());            //cada dispositivo
                    ids_dispositivos.add(dispositivo.getID());
                    conexion_dispositivos.add(dispositivo.isConnected() ? IniciarSesionActivity.this.getString(R.string.online) :
                            IniciarSesionActivity.this.getString(R.string.offline));
                }

                Intent intent = new Intent(getApplicationContext(), DispositivosActivity.class);    //Enviamos la informacion obtenida
                intent.putStringArrayListExtra("nombres_dispositivos", nombres_dispositivos);       //a la activity DispositivosActivity
                intent.putStringArrayListExtra("ids_dispositivos", ids_dispositivos);
                intent.putStringArrayListExtra("conexion_dispositivos", conexion_dispositivos);
                intent.putExtra("directorio_app", directorio_app.toString() + File.separator + email);
                finish();
                Log.i(Tag, "Inicio de sesion exitoso, cambio de Activity a DispositivosActivity");
                startActivity(intent);
            }

            @Override
            public void onFailure(SparkCloudException e) {
                Util.toast(IniciarSesionActivity.this, getString(R.string.sesion_no_iniciada));
                boton_iniciar_sesion.setBackgroundColor(ContextCompat.getColor(IniciarSesionActivity.this, R.color.colorPrimary));
                boton_iniciar_sesion.setTextColor(ContextCompat.getColor(IniciarSesionActivity.this, R.color.blanco));
                boton_iniciar_sesion.setClickable(true);
                Log.w(Tag, "No se pudo iniciar sesion");
            }
        });
    }

    /**
     * prepararDirectorioApp: obtiene la ruta a la carpeta de la app y analiza que exista, si no es asi la crea
     */
    private void prepararDirectorioApp() {
        directorio_app = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + getString(R.string.app_name));
        Util.comprobarDirectorio(this, directorio_app); //Comprobamos que exista el directorio, si no es asi, lo creamos
    }

    /**
     * prepararEditTexts: Obtiene las vistas de los cuadros donde se escribiran el correo y la contrasena y los prepara (mientras
     * el usuario va escribiendo su correo, aparece una lista con las opciones de correos ya antes utilizados), tambien inicia el evento
     * para que al pulsar enter al escribir el correo, situe el cursos directamente en el EditText de la contrasena
     */
    private void prepararEditTexts() {
        AutoCompleteTextView et_email = (AutoCompleteTextView) findViewById(R.id.etEmail);
        et_contrasena = (EditText) findViewById(R.id.etPass);

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
}