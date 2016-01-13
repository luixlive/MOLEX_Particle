package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Clase EditarModulos: activity que le permite la usuario cambiar las imagenes y nombres de los modulos de un dispositivo seleccionado
 * asi como el nombre de dicho dispositivo (este no se guarda en el almacenamiento, si no en la nube de Particle)
 * Created by LUIS ALFONSO on 23/12/2015.
 */
public class EditarModulos extends AppCompatActivity {

    private final static double RELACION_WIDTH_IMAGEN = 1.0 / 6;    //Macros
    private final static int REQUEST_CODE = 1216;

    private EditText nombre_dispositivo;        //Vistasa del layout
    private EditText nombre_modulo_1;
    private EditText nombre_modulo_2;
    private EditText nombre_modulo_3;

    private String nombre_dispositivo_viejo;
    private String directorio_modulos;
    private boolean almacenamiento_modulos_posible;
    private boolean configuracion_inicial;      //Se pone en true si es la primera vez que se configuran los modulos
    private String id_dispositivo;

    private ImageButton boton_presionado;
    private int numero_boton_presionado;

    private int ancho_imagen;
    private Bitmap imagenes[];

    private boolean imagenes_modificadas[] = new boolean[3]; //Banderas para saber que imagen se cambio y cual no al guardar

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_modulos);

        for (int index = 0; index < Util.NUMERO_MODULOS; index++) imagenes_modificadas[index] = false;

        calcularAnchoImagenes();
        recibirInformacionYActualizarVistas();
        declararEventosBotones();
    }

    /**
     * guardarCambios: se llama cuando el usuario pulsa guardar, guarda las imagenes como jpeg en el directorio de los modulos
     * dentro del directorio de la app, un texto en el mismo lugar con los nombres de los modulos y el nombre del dispositivo lo
     * guarda en la nube de particle
     * @param boton_guardar: vista del boton guardar
     */
    public void guardarCambios(View boton_guardar){
        boton_guardar.setBackgroundColor(ContextCompat.getColor(this, R.color.blanco));
        ((Button)boton_guardar).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

        String nombres_modulos_nuevos[] = new String[Util.NUMERO_MODULOS];
        boolean guardado_exitoso = false;

        if(almacenamiento_modulos_posible) {
            for (int index = 0; index < Util.NUMERO_MODULOS; index++) {
                if (imagenes_modificadas[index]) {
                    File ruta_imagen = new File(directorio_modulos + File.separator + (index + 1) + getString(R.string.tipo_imagen));
                    FileOutputStream archivo;   //Guardamos las imagenes
                    try {
                        archivo = new FileOutputStream(ruta_imagen);
                        if (imagenes[index].getByteCount() > Util.MAX_TAM_IMAGEN_MODULO) {
                            imagenes[index].compress(Bitmap.CompressFormat.JPEG, (Util.MAX_TAM_IMAGEN_MODULO * 100 / imagenes[index].getByteCount()),
                                    archivo);
                        } else
                            imagenes[index].compress(Bitmap.CompressFormat.JPEG, 100, archivo);
                        archivo.close();
                        Log.i(Util.TAG_EM, "Se cambio la imagen " + index + " exitosamente");
                    } catch (Exception e) {
                        Log.e(Util.TAG_EM, "No se completo exitosamente el guardado de imagen" + index + "\n" + e.toString());
                    }
                    finally {
                        imagenes[index].recycle();
                    }
                }
            }

            //Actualizamos el nombre del dispositivo en la nube de Particle si el usuario lo cambio
            String nombre_dispositivo_nuevo = nombre_dispositivo.getText().toString();
            if(!nombre_dispositivo_nuevo.equals(nombre_dispositivo_viejo)) {
                Util.ParticleAPI.cambiarNombreDispositivo(id_dispositivo, nombre_dispositivo_nuevo);
            }

            File documento_con_nombres = new File(directorio_modulos, getString(R.string.nombre_archivo_nombres_modulos));
            FileOutputStream stream;        //Guardamos un txt con los nombres de los modulos en el orden correcto para facilitar la lectura
            nombres_modulos_nuevos[0] = nombre_modulo_1.getText().toString();
            nombres_modulos_nuevos[1] = nombre_modulo_2.getText().toString();
            nombres_modulos_nuevos[2] = nombre_modulo_3.getText().toString();
            try {
                stream = new FileOutputStream(documento_con_nombres);
                stream.write((nombres_modulos_nuevos[0] + "\n").getBytes());
                stream.write((nombres_modulos_nuevos[1] + "\n").getBytes());
                stream.write((nombres_modulos_nuevos[2]).getBytes());
                stream.close();
                guardado_exitoso = true;
            } catch (Exception e) {
                Log.e(Util.TAG_EM, e.toString());
                Util.toast(this, getString(R.string.cambios_no_guardados));
            }
        } else Util.toast(this, getString(R.string.cambios_no_guardados));

        boton_guardar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        ((Button) boton_guardar).setTextColor(ContextCompat.getColor(this, R.color.blanco));

        if (guardado_exitoso) {
            Intent intent = new Intent();
            intent.putExtra("nuevos_nombres", nombres_modulos_nuevos);
            setResult(RESULT_OK, intent);
        } else setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * calcularAnchoImagenes: calcula el ancho en pixeles de las imagenes para mostrarlas en pantalla (son mas pequenas que los
     * avatares)
     */
    private void calcularAnchoImagenes() {
        DisplayMetrics metrics = new DisplayMetrics();                  //Obtenemos las medidas de la pantalla del celular
        getWindowManager().getDefaultDisplay().getMetrics(metrics);     //para ajustar lo ancho de las imagenes
        ancho_imagen = (int)(metrics.widthPixels * RELACION_WIDTH_IMAGEN);
    }

    /**
     * recibirInformacionYActualizarVistas: captura la informacion recibida por el intent de la activity anterior y guarda la informacion
     * en variables globales, tambien utiliza esa informacion para poner las vistas de nombre de dispositivo, nombres de modulos e
     * imagenes de modulos
     */
    private void recibirInformacionYActualizarVistas() {
        nombre_dispositivo = (EditText) findViewById(R.id.etNombreDispositivoEditarModulos);
        nombre_modulo_1 = (EditText) findViewById(R.id.etModulo1);
        nombre_modulo_2 = (EditText) findViewById(R.id.etModulo2);
        nombre_modulo_3 = (EditText) findViewById(R.id.etModulo3);

        Intent intent = getIntent();
        configuracion_inicial = intent.getBooleanExtra("configuracion_inicial", false); //Actualizamos las vistas capturando los intents
        id_dispositivo = intent.getStringExtra("id_dispositivo");
        nombre_dispositivo_viejo = intent.getStringExtra("nombre_dispositivo");
        nombre_dispositivo.setText(nombre_dispositivo_viejo);
        nombre_modulo_1.setText(intent.getStringExtra("nombre_modulo_1"));
        nombre_modulo_2.setText(intent.getStringExtra("nombre_modulo_2"));
        nombre_modulo_3.setText(intent.getStringExtra("nombre_modulo_3"));
        directorio_modulos = intent.getStringExtra("directorio_modulos");
        almacenamiento_modulos_posible = intent.getBooleanExtra("almacenamiento_modulos_posible", false);
        imagenes = new Bitmap[Util.NUMERO_MODULOS];

        if (almacenamiento_modulos_posible) {       //Leemos los bitmaps
            File directorio_modulo;
            for (int index = 0; index < Util.NUMERO_MODULOS; index++) {
                directorio_modulo = new File(directorio_modulos + File.separator + (index + 1) + getString(R.string.tipo_imagen));
                if (directorio_modulo.exists())
                    imagenes[index] = Util.obtenerImagenReducida(directorio_modulo.getPath(), ancho_imagen, ancho_imagen);
                else imagenes[index] = BitmapFactory.decodeResource(getResources(), R.mipmap.nulo);
            }
        }
        ((ImageButton) findViewById(R.id.ibModulo1)).setImageBitmap(escalarImagen(imagenes[0]));
        ((ImageButton) findViewById(R.id.ibModulo2)).setImageBitmap(escalarImagen(imagenes[1]));
        ((ImageButton) findViewById(R.id.ibModulo3)).setImageBitmap(escalarImagen(imagenes[2]));
    }

    /**
     * declararEventosBotones: se declara un evento para cada boton de actualizar imagen, esto con el fin de guardar la vista decuada
     * y el numero del boton presionado para guardar la informacion
     */
    private void declararEventosBotones() {
        ImageButton boton_modulo_1 = (ImageButton)findViewById(R.id.ibModulo1);
        ImageButton boton_modulo_2 = (ImageButton)findViewById(R.id.ibModulo2);
        ImageButton boton_modulo_3 = (ImageButton)findViewById(R.id.ibModulo3);

        boton_modulo_1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    boton_presionado = (ImageButton) v;
                    numero_boton_presionado = 0;
                    cambiarImagen();
                }
                return true;
            }
        });
        boton_modulo_2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    boton_presionado = (ImageButton) v;
                    numero_boton_presionado = 1;
                    cambiarImagen();
                }
                return true;
            }
        });
        boton_modulo_3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    boton_presionado = (ImageButton) v;
                    numero_boton_presionado = 2;
                    cambiarImagen();
                }
                return true;
            }
        });
    }

    /**
     * cambiarImagen: se inicia un intent solicitando al usuario que seleccione una imagen de su galeria
     */
    private void cambiarImagen() {
        imagenes_modificadas[numero_boton_presionado] = true;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)    //Si se contesta nuestro intent
            try {
                InputStream stream = getContentResolver().openInputStream(data.getData());
                Bitmap imagen_nueva = BitmapFactory.decodeStream(stream, new Rect(0, 0, ancho_imagen, ancho_imagen), new BitmapFactory.Options());
                if (stream != null) stream.close();

                imagenes[numero_boton_presionado] = imagen_nueva;
                boton_presionado.setImageBitmap(escalarImagen(imagen_nueva));                     //Sustituimos el avatar
            } catch (Exception e) {
                Log.e(Util.TAG_EM, e.toString());
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * escalarImagen: ajusta el tamano de las imagenes para que se vean mejor en la pantalla del smartphone en uso
     * @param imagen: imagen que se quiere ajustar
     * @return un Bitmap con la imagen final
     */
    private Bitmap escalarImagen(Bitmap imagen) {
        if (imagen == null) return null;
        int alto_avatar = (imagen.getHeight() * ancho_imagen) / imagen.getWidth();
        return Bitmap.createScaledBitmap(imagen, ancho_imagen, alto_avatar, false);
    }

    @Override
    public void onBackPressed() {
        if (configuracion_inicial) return;  //Si es la primera vez que se configura no dejamos al usuario regresar a la activity anterior
        super.onBackPressed();              //hasta guardar
    }
}
