package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Clase HiloActualizarDispositivos: implementa Runnable y corre un hilo en segundo plano que se encarga de verificar cada 5 segundos
 * las conexiones y nombres de los dispositivos en la Particle Cloud, y llama un metodo del hilo principal que se encarga de
 * actualizar la interfaz grafica
 * Created by LUIS ALFONSO on 21/12/2015.
 */
public class HiloActualizarDispositivos implements Runnable {

    private volatile Activity activity;
    private volatile ArrayList<Boolean> conexion_dispositivos;
    private volatile ArrayList<String> nombre_dispositivos;
    private volatile ArrayList<String> ids_dispositivos;
    private volatile boolean interrupted = false;            //Bandera que se utiliza para informar al hilo cuando debe terminar

    /**
     * HiloActualizarDispositivos: constructor, recibe un objeto tipo activiy que representa el contexto actual de la app
     * @param activity: contexto de la app, se utiliza obtener recursos de la carpeta R y vistas de los layouts
     */
    public HiloActualizarDispositivos(Activity activity){
        this.activity = activity;
        conexion_dispositivos = new ArrayList<>();
        nombre_dispositivos = new ArrayList<>();
        ids_dispositivos = new ArrayList<>();
    }

    @Override
    public void run() {
        Semaphore semaforo = new Semaphore(1, true);
        //Mientras no sea interrumpido, cada 3 segundos pedira los dispositivos de Particle Cloud para analizar su conexion.
        //Como el acceso a la Particle Cloud se hace tambien desde otro hilo, evitamos generar multiples hilos utilizando un
        //semaforo para que solo pueda existir un hilo Async y un hilo HiloActualizarDispositivos a la vez
        while(!interrupted) {
            Util.ParticleAPI.actualizarInformacionDispositivos(this, interrupted, semaforo);
            try {
                semaforo.acquire();
                Thread.sleep(Util.TIEMPO_MUERTO_HILO_CONEXION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            semaforo.release();
        }
        Log.i(Util.TAG_HAD, "Se detuvo el hilo para actualizar la conexion");
    }

    public void actualizacionExitosa(ArrayList<String> ultimos_nombres_dispositivos, ArrayList<String> ultimos_ids_dispositivos,
                                     ArrayList<Boolean> ultimas_conexiones_dispositivos) {
        conexion_dispositivos = ultimas_conexiones_dispositivos;      //Se limpian los nombres y estados de conexion antiguos
        nombre_dispositivos = ultimos_nombres_dispositivos;        //para obtener los actualizados
        ids_dispositivos = ultimos_ids_dispositivos;
        actualizarDispositivos();
    }

    /**
     * actualizarConexiones: se llama para avisar al hilo principal que se han actualizado los estados de los dispositivos
     */
    private void actualizarDispositivos(){
        if (!interrupted)
            ((DispositivosActivity)activity).actualizarDispositivos(nombre_dispositivos, conexion_dispositivos, ids_dispositivos);
    }

    /**
     * interrumpir: se llama desde otra clase para interrumpir el hilo cuando ya no se necesite estar actualizando
     */
    public void interrumpir(){
        interrupted = true;
    }

    /**
     * setActivity: siempre que se cambie el contexto de la aplicacion hay que actualizar este campo para que llame al metodo
     * actualizarConexiones en la activity actual y no una obsoleta
     * @param activity: nueva activity con el contexto actual
     */
    public void setActivity(Activity activity){
        this.activity = activity;
    }
}
