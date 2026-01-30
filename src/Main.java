import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class Main {

    static class IO {
        static void print(String s) { System.out.print(s); }
        static void println(String s) { System.out.println(s); }
        static void println() { System.out.println(); }
    }

    static record Score(String nombre, int intentos, int maxIntentos, int minimo, int maximo) {}

    static final ArrayList<Score> scoreboard = new ArrayList<>();
    static final Path SCORE_PATH = Paths.get("scoreboard.txt");
    static final int MAX_REGISTROS = 100;

    public static void main(String[] args) {
        Random aleatorio = new Random();

        cargarScoreboard();

        try (Scanner entrada = new Scanner(System.in)) {
            while (true) {
                IO.println("Juego: Adivina el numero.");
                IO.println("Pista: ↑ SUBE (el secreto es mayor), ↓ BAJA (el secreto es menor).");

                String nombre = leerLineaNoVacia(entrada, "Nombre: ");

                int minimo = leerInt(entrada, "Minimo: ");
                int maximo = leerInt(entrada, "Maximo: ");
                while (minimo >= maximo) {
                    IO.println("Rango invalido. El minimo debe ser menor que el maximo.");
                    minimo = leerInt(entrada, "Minimo: ");
                    maximo = leerInt(entrada, "Maximo: ");
                }

                Score resultado = jugarRonda(entrada, aleatorio, nombre, minimo, maximo);

                if (resultado != null) {
                    scoreboard.add(resultado);
                    ordenarScoreboard();
                    recortarScoreboard();
                    guardarScoreboard();
                    mostrarScoreboard();
                } else {
                    IO.println("Partida terminada. No se guardo en el scoreboard.");
                }

                String op = leerLineaNoVacia(entrada, "¿Jugar otra vez? (s/n): ");
                if (!op.equalsIgnoreCase("s")) break;

                IO.println();
            }
        }
    }

    static Score jugarRonda(Scanner entrada, Random aleatorio, String nombre, int minimo, int maximo) {
        int secreto = aleatorio.nextInt((maximo - minimo) + 1) + minimo;

        int minSugerido = minimo;
        int maxSugerido = maximo;

        int totalOpciones = (maximo - minimo) + 1;
        int maxIntentos = calcularMaxIntentos(totalOpciones);

        ArrayList<Integer> historial = new ArrayList<>();
        int intentos = 0;

        String sugerencia = "↑ SUBE / ↓ BAJA";

        IO.println();
        IO.println("Rango inicial: " + minimo + " a " + maximo);
        IO.println("Intentos disponibles: " + maxIntentos);
        imprimirRangoSugerido(minSugerido, maxSugerido);
        IO.println();

        while (true) {
            if (intentos >= maxIntentos) {
                IO.println("Se acabaron los intentos.");
                IO.println("El numero era: " + secreto);
                return null;
            }

            IO.println("Intento " + (intentos + 1) + " de " + maxIntentos);

            int numero = leerInt(entrada, "Numero (" + sugerencia + "): ");

            if (!insertarOrdenadoSinRepetir(historial, numero)) {
                IO.println("Ese numero ya fue probado. Intenta otro.");
                IO.println();
                continue;
            }

            intentos++;

            if (numero == secreto) {
                IO.println("Correcto.");
                IO.println("Respuesta: " + secreto);
                IO.println("Total de intentos: " + intentos);
                IO.println();
                return new Score(nombre, intentos, maxIntentos, minimo, maximo);
            }

            boolean esMenorQueSecreto = numero < secreto;
            int diferencia = Math.abs(numero - secreto);

            String flecha = esMenorQueSecreto ? "↑" : "↓";
            String dir = esMenorQueSecreto ? "SUBE" : "BAJA";
            String dist = pistaDistancia(diferencia, minimo, maximo);

            IO.println("Pista: " + flecha + " " + dir + " | " + dist);

            sugerencia = esMenorQueSecreto ? "↑ SUBE" : "↓ BAJA";

            if (esMenorQueSecreto) minSugerido = Math.max(minSugerido, numero + 1);
            else maxSugerido = Math.min(maxSugerido, numero - 1);

            imprimirRangoSugerido(minSugerido, maxSugerido);
            IO.println("Probados: " + historial);
            IO.println();
        }
    }

    static String pistaDistancia(int diferencia, int minimoInicial, int maximoInicial) {
        int rangoInicial = Math.max(1, maximoInicial - minimoInicial);
        double r = diferencia / (double) rangoInicial;

        if (r <= 0.02) return "Muy cerca.";
        if (r <= 0.05) return "Cerca.";
        if (r <= 0.10) return "Media distancia.";
        if (r <= 0.20) return "Lejos.";
        return "Muy lejos.";
    }

    static int calcularMaxIntentos(int opciones) {
        if (opciones <= 1) return 1;
        double v = Math.log(opciones) / Math.log(2);
        return (int) Math.ceil(v) + 1;
    }

    static void imprimirRangoSugerido(int minActual, int maxActual) {
        if (minActual > maxActual) {
            IO.println("Rango sugerido: (sin opciones).");
            return;
        }
        if (minActual == maxActual) {
            IO.println("Rango sugerido: solo queda " + minActual + ".");
            return;
        }
        IO.println("Rango sugerido: " + minActual + " a " + maxActual + ".");
    }

    static int leerInt(Scanner entrada, String texto) {
        while (true) {
            IO.print(texto);
            if (entrada.hasNextInt()) {
                int v = entrada.nextInt();
                entrada.nextLine();
                return v;
            }
            entrada.nextLine();
            IO.println("Entrada invalida. Escribe un numero entero.");
        }
    }

    static String leerLineaNoVacia(Scanner entrada, String texto) {
        while (true) {
            IO.print(texto);
            String s = entrada.nextLine();
            if (!s.trim().isEmpty()) return s.trim();
            IO.println("Entrada invalida.");
        }
    }

    static boolean insertarOrdenadoSinRepetir(ArrayList<Integer> lista, int numero) {
        int pos = buscarPosicion(lista, numero);
        if (pos < lista.size() && lista.get(pos) == numero) return false;
        lista.add(pos, numero);
        return true;
    }

    static int buscarPosicion(ArrayList<Integer> lista, int numero) {
        int idx = Collections.binarySearch(lista, numero);
        return (idx >= 0) ? idx : (-idx - 1);
    }

    static double eficiencia(Score s) {
        int opciones = (s.maximo() - s.minimo()) + 1;
        int base = (opciones <= 1) ? 1 : (int) Math.ceil(Math.log(opciones) / Math.log(2)) + 1;
        return s.intentos() / (double) base;
    }

    static void ordenarScoreboard() {
        scoreboard.sort(
                Comparator.comparingDouble(Main::eficiencia)
                        .thenComparingInt(Score::intentos)
                        .thenComparingInt(s -> -((s.maximo() - s.minimo()) + 1))
        );
    }

    static void recortarScoreboard() {
        while (scoreboard.size() > MAX_REGISTROS) {
            scoreboard.remove(scoreboard.size() - 1);
        }
    }

    static void cargarScoreboard() {
        if (!Files.exists(SCORE_PATH)) return;

        try {
            List<String> lineas = Files.readAllLines(SCORE_PATH, StandardCharsets.UTF_8);
            for (String linea : lineas) {
                if (linea == null) continue;
                String t = linea.trim();
                if (t.isEmpty()) continue;

                String[] p = t.split("\\|", -1);

                if (p.length == 5) {
                    String nombre = p[0].trim();
                    int intentos = Integer.parseInt(p[1].trim());
                    int maxIntentos = Integer.parseInt(p[2].trim());
                    int minimo = Integer.parseInt(p[3].trim());
                    int maximo = Integer.parseInt(p[4].trim());
                    if (!nombre.isEmpty() && intentos > 0 && minimo < maximo) {
                        scoreboard.add(new Score(nombre, intentos, maxIntentos, minimo, maximo));
                    }
                } else if (p.length == 4) {
                    String nombre = p[0].trim();
                    int intentos = Integer.parseInt(p[1].trim());
                    int minimo = Integer.parseInt(p[2].trim());
                    int maximo = Integer.parseInt(p[3].trim());
                    if (!nombre.isEmpty() && intentos > 0 && minimo < maximo) {
                        scoreboard.add(new Score(nombre, intentos, 0, minimo, maximo));
                    }
                }
            }
            ordenarScoreboard();
            recortarScoreboard();
        } catch (IOException | NumberFormatException e) {
            IO.println("No se pudo leer el scoreboard.");
        }
    }

    static void guardarScoreboard() {
        ordenarScoreboard();
        recortarScoreboard();

        ArrayList<String> lineas = new ArrayList<>();
        for (Score s : scoreboard) {
            lineas.add(s.nombre() + "|" + s.intentos() + "|" + s.maxIntentos() + "|" + s.minimo() + "|" + s.maximo());
        }

        try {
            Files.write(
                    SCORE_PATH,
                    lineas,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            IO.println("No se pudo guardar el scoreboard.");
        }
    }

    static void mostrarScoreboard() {
        IO.println("--- SCOREBOARD (Top 5) ---");
        int limite = Math.min(5, scoreboard.size());
        for (int i = 0; i < limite; i++) {
            Score s = scoreboard.get(i);
            int rango = (s.maximo() - s.minimo()) + 1;
            String intentosTxt = (s.maxIntentos() > 0) ? (s.intentos() + "/" + s.maxIntentos()) : String.valueOf(s.intentos());
            String efTxt = String.format(Locale.ROOT, "%.2f", eficiencia(s));
            System.out.printf("%d) %s | intentos %s | rango %d | eficiencia %s%n",
                    i + 1, s.nombre(), intentosTxt, rango, efTxt);
        }
        IO.println("---------------------------");
        IO.println("Guardado en: " + SCORE_PATH.toAbsolutePath());
    }
}
