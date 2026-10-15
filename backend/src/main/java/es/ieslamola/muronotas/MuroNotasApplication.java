package es.ieslamola.muronotas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MuroNotasApplication {

    // Variables sin valor por defecto: sin ellas no tiene sentido arrancar
    private static final String[] VARIABLES_OBLIGATORIAS = {"DB_HOST", "DB_PASSWORD"};

    public static void main(String[] args) {
        comprobarVariablesObligatorias();
        SpringApplication.run(MuroNotasApplication.class, args);
    }

    private static void comprobarVariablesObligatorias() {
        for (String variable : VARIABLES_OBLIGATORIAS) {
            String valor = System.getenv(variable);
            if (valor == null || valor.isBlank()) {
                System.err.println("ERROR: falta la variable de entorno " + variable + ".");
                System.err.println("Ponla en el fichero .env (copia .env.example) y vuelve a arrancar.");
                System.exit(1);
            }
        }
    }
}
