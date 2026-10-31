package com.proyecto.servicios;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;



@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@Slf4j

public class App implements CommandLineRunner {

    static {
        // HttpURLConnection reenvia silenciosamente un POST si la conexion se corta. En operaciones financieras
        // eso puede duplicar un cargo: el reintento solo debe ocurrir de forma explicita (idempotente) en la app.
        System.setProperty("sun.net.http.retryPost", "false");
    }

    @Autowired
    private ApplicationContext context;


    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        //	displayInfo(context.getBean(BuildProperties.class));
    }

    private static void displayInfo(BuildProperties buildProperties) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String out = formatter.format(buildProperties.getTime());
        log.info("Nombre artefacto: " + buildProperties.getName() + "\n"
                + "Versión: " + buildProperties.getVersion() + "\n"
                + "Fecha Compilación: " + out + "\n"
                + "Artefacto: " + buildProperties.getArtifact() + "\n"
                + "Grupo: " + buildProperties.getGroup());


    }
}
