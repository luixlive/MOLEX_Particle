package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Clase Util: Contiene funciones publicas y estaticas que se utilizan de manera repetitiva en la app, se agrupan aqui para acceder a
 * ellas de forma estatica y evitar codigo repetitivo.
 * Created by LUIS ALFONSO on 30/12/2015.
 */
public class Util {

    private final static String Tag = "SP Util";

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
                Log.i(Tag, "No existe direcotirio, activity: " + activity.getLocalClassName());
                if (!directorio.mkdirs()) {                             //Si no se puede crear
                    Log.w(Tag, "No se pudo crear un directorio, activity" + activity.getLocalClassName());
                    return false;
                } else return true;
            } else return true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(estado_almacenamiento)) {   //Si existe un almacenamiento pero
            toast(activity, activity.getString(R.string.almacenamiento_no_escritura));     //solo nos permite leer y no escribir
            Log.w(Tag, "Solo es posible leer en el almacenamiento externo, activity" + activity.getLocalClassName());
            return false;
        }
        else {
            Log.w(Tag, "No se puede leer ni escribir en el almacenamiento externo, activity: " + activity.getLocalClassName());
            toast(activity, activity.getString(R.string.almacenamiento_no_escritura_lectura));
            return false;
        }
    }

    public static void toast(Activity activity, String mensaje){
        Toast.makeText(activity.getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

}
