package com.pruebaparticle.luisalfonso.molexparticle;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

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
    private volatile boolean interrupted = false;            //Bandera que se utiliza para informar al hilo cuando debe terminar
    private volatile Semaphore semaforo = new Semaphore(1, true);   //Semaforo que ayuda con la sincronizacion entre hilos
    private List<ParticleDevice> dispositivos;

    /**
     * HiloActualizarDispositivos: constructor, recibe un objeto tipo activiy que representa el contexto actual de la app
     * @param activity: contexto de la app, se utiliza obtener recursos de la carpeta R y vistas de los layouts
     */
    public HiloActualizarDispositivos(Activity activity){
        this.activity = activity;
        conexion_dispositivos = new ArrayList<>();
        nombre_dispositivos = new ArrayList<>();
    }

    @Override
    public void run() {
        //Mientras no sea interrumpido, cada 5 segundos pedira los dispositivos de Particle Cloud para analizar su conexion.
        //Como el acceso a la Particle Cloud se hace tambien desde otro hilo, evitamos generar multiples hilos utilizando un
        //semaforo para que solo pueda existir un hilo Async y un hilo HiloActualizarDispositivos a la vez
        while(!interrupted) {
            Async.executeAsync(Util.ParticleAPI.obtenerNubeParticle(), new Async.ApiWork<ParticleCloud, List<ParticleDevice>>() {
                @Override
                public List<ParticleDevice> callApi(@NonNull ParticleCloud nube_particle) throws ParticleCloudException, IOException {
                    try {
                        semaforo.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!interrupted) dispositivos = nube_particle.getDevices();
                    semaforo.release();
                    return dispositivos;
                }
                @Override
                public void onSuccess(List<ParticleDevice> dispositivos) {
                    conexion_dispositivos.clear();      //Se limpian los nombres y estados de conexion antiguos
                    nombre_dispositivos.clear();        //para obtener los actualizados
                    for (int index = 0; index < dispositivos.size(); index++) {
                        conexion_dispositivos.add(dispositivos.get(index).isConnected());
                        nombre_dispositivos.add(dispositivos.get(index).getName());
                    }
                    actualizarDispositivos();
                    dispositivos.clear();
                }
                @Override
                public void onFailure(ParticleCloudException exception) {
                    dispositivos.clear();
                }
            });
            try {
                semaforo.acquire();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            semaforo.release();
        }
        Log.i(Util.TAG_HAD, "Se detuvo el hilo para actualizar la conexion");
    }

    /**
     * interrumpir: se llama desde otra clase para interrumpir el hilo cuando ya no se necesite estar actualizando
     */
    public void interrumpir(){
        interrupted = true;
    }

    /**
     * actualizarConexiones: se llama para avisar al hilo principal que se han actualizado los estados de los dispositivos
     */
    private void actualizarDispositivos(){
        if (!interrupted)
            ((DispositivosActivity)activity).actualizarDispositivos(nombre_dispositivos, conexion_dispositivos);
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
