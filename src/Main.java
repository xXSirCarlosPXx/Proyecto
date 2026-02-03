import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Main {

    static final class C {
        static final String ESC = "\u001B[";
        static final String RESET = ESC + "0m";
        static final String BOLD = ESC + "1m";
        static final String DIM = ESC + "2m";

        static String fg256(int n) { return ESC + "38;5;" + n + "m"; }
        static String bg256() { return ESC + "48;5;" + 236 + "m"; }

        static final String TITLE = BOLD + fg256(51);
        static final String ACCENT = BOLD + fg256(213);
        static final String OK = BOLD + fg256(82);
        static final String WARN = BOLD + fg256(214);
        static final String BAD = BOLD + fg256(203);
        static final String INFO = BOLD + fg256(117);
        static final String MUTED = DIM + fg256(245);

        static final String ARROW_UP = BOLD + fg256(82);
        static final String ARROW_DOWN = BOLD + fg256(203);

        static final String PANEL = bg256() + fg256(255);
        static final String PANEL_EDGE = fg256(39);

        static void clear() { System.out.print(ESC + "H" + ESC + "2J"); System.out.flush(); }
    }

    record Score(String nombre, int intentos, int maxIntentos, int minimo, int maximo) {}

    static final ArrayList<Score> marcador = new ArrayList<>();
    static final Path SCORE_PATH = Paths.get("scoreboard.txt");
    static final int MAX_REGISTROS = 100;

    static void main() {
        Random aleatorio = new Random();
        cargarMarcador();

        try (Scanner entrada = new Scanner(System.in)) {
            while (true) {
                C.clear();

                IO.println(C.TITLE + "=== ADIVINA EL NÚMERO ===" + C.RESET);
                IO.println(C.MUTED + "Pista: ↑ SUBE (el secreto es mayor), ↓ BAJA (el secreto es menor)." + C.RESET);
                IO.println();
                IO.println(C.PANEL_EDGE + "┌──────────────────────────────┐" + C.RESET);
                IO.println(C.PANEL_EDGE + "│ " + C.RESET + C.PANEL + "1) Jugar                     " + C.RESET + C.PANEL_EDGE + "│" + C.RESET);
                IO.println(C.PANEL_EDGE + "│ " + C.RESET + C.PANEL + "2) Ver marcador              " + C.RESET + C.PANEL_EDGE + "│" + C.RESET);
                IO.println(C.PANEL_EDGE + "│ " + C.RESET + C.PANEL + "3) Ver estadísticas          " + C.RESET + C.PANEL_EDGE + "│" + C.RESET);
                IO.println(C.PANEL_EDGE + "│ " + C.RESET + C.PANEL + "4) Salir                     " + C.RESET + C.PANEL_EDGE + "│" + C.RESET);
                IO.println(C.PANEL_EDGE + "└──────────────────────────────┘" + C.RESET);
                IO.println();

                int op = leerIntRango(entrada, C.ACCENT + "Elige una opción (1-4): " + C.RESET);
                IO.println();

                switch (op) {
                    case 1 -> jugar(entrada, aleatorio);
                    case 2 -> { mostrarMarcador(); pausar(entrada); }
                    case 3 -> { mostrarEstadisticas(); pausar(entrada); }
                    case 4 -> { IO.println(C.INFO + "Hasta luego." + C.RESET); return; }
                }
            }
        }
    }

    static void pausar(Scanner entrada) {
        IO.println();
        IO.print(C.MUTED + "Enter para continuar..." + C.RESET);
        entrada.nextLine();
    }

    static void jugar(Scanner entrada, Random aleatorio) {
        C.clear();
        IO.println(C.TITLE + "=== NUEVA PARTIDA ===" + C.RESET);
        IO.println();

        String nombre = leerLineaNoVacia(entrada, C.ACCENT + "Nombre: " + C.RESET);

        int minimo = leerInt(entrada, C.ACCENT + "Mínimo: " + C.RESET);
        int maximo = leerInt(entrada, C.ACCENT + "Máximo: " + C.RESET);
        while (minimo >= maximo) {
            IO.println(C.BAD + "Rango inválido. El mínimo debe ser menor que el máximo." + C.RESET);
            minimo = leerInt(entrada, C.ACCENT + "Mínimo: " + C.RESET);
            maximo = leerInt(entrada, C.ACCENT + "Máximo: " + C.RESET);
        }

        Score resultado = jugarRonda(entrada, aleatorio, nombre, minimo, maximo);

        if (resultado != null) {
            marcador.add(resultado);
            ordenarMarcador();
            recortarMarcador();
            guardarMarcador();
            IO.println(C.OK + "Partida guardada en el marcador." + C.RESET);
            IO.println();
            mostrarMarcador();
        } else {
            IO.println(C.WARN + "Partida terminada. No se guardó en el marcador." + C.RESET);
        }

        pausar(entrada);
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

        //IO.println(C.INFO + "Rango inicial: " + minimo + " a " + maximo + C.RESET);
        IO.println(C.INFO + "Intentos disponibles: " + maxIntentos + C.RESET);
        //imprimirRangoSugerido(minSugerido, maxSugerido);
        IO.println();

        while (true) {
            if (intentos >= maxIntentos) {
                IO.println(C.BAD + "Se acabaron los intentos." + C.RESET);
                IO.println(C.BAD + "El número era: " + secreto + C.RESET);
                return null;
            }

            IO.println(C.ACCENT + "Intento " + (intentos + 1) + " de " + maxIntentos + C.RESET);

            int numero = leerInt(entrada, C.ACCENT + "Número (" + sugerencia + "): " + C.RESET);

            if (!insertarOrdenadoSinRepetir(historial, numero)) {
                IO.println(C.WARN + "Ese número ya fue probado. Intenta otro." + C.RESET);
                IO.println();
                continue;
            }

            intentos++;

            if (numero == secreto) {
                IO.println(C.OK + "Correcto." + C.RESET);
                IO.println(C.OK + "Respuesta: " + secreto + C.RESET);
                IO.println(C.OK + "Total de intentos: " + intentos + C.RESET);
                IO.println();
                return new Score(nombre, intentos, maxIntentos, minimo, maximo);
            }

            boolean esMenorQueSecreto = numero < secreto;
            int diferencia = Math.abs(numero - secreto);

            String flecha = esMenorQueSecreto ? (C.ARROW_UP + "↑" + C.RESET) : (C.ARROW_DOWN + "↓" + C.RESET);
            String dir = esMenorQueSecreto ? (C.ARROW_UP + "SUBE" + C.RESET) : (C.ARROW_DOWN + "BAJA" + C.RESET);
            String dist = pistaDistancia(diferencia, minimo, maximo);

            IO.println(C.INFO + "Pista: " + C.RESET + flecha + " " + dir + C.MUTED + " | " + dist + C.RESET);

            sugerencia = esMenorQueSecreto ? "↑ SUBE" : "↓ BAJA";

            if (esMenorQueSecreto) minSugerido = Math.max(minSugerido, numero + 1);
            else maxSugerido = Math.min(maxSugerido, numero - 1);

            //(minSugerido, maxSugerido);
            IO.println(C.MUTED + "Probados: " + historial + C.RESET);
            IO.println();
        }
    }

    static String pistaDistancia(int diferencia, int minimoInicial, int maximoInicial) {
        int rangoInicial = Math.max(1, maximoInicial - minimoInicial);
        double r = diferencia / (double) rangoInicial;

        if (r <= 0.02) return "Muy cerca.";
        if (r <= 0.05) return "Cerca.";
        if (r <= 0.10) return "A media distancia.";
        if (r <= 0.20) return "Lejos.";
        return "Muy lejos.";
    }

    static int calcularMaxIntentos(int opciones) {
        if (opciones <= 1) return 1;
        double v = Math.log(opciones) / Math.log(2);
        return (int) Math.ceil(v) + 1;
    }

    /*static void imprimirRangoSugerido(int minActual, int maxActual) {
        if (minActual > maxActual) {
            IO.println(C.WARN + "Rango sugerido: (sin opciones)." + C.RESET);
            return;
        }
        if (minActual == maxActual) {
            IO.println(C.INFO + "Rango sugerido: " + C.OK + "solo queda " + minActual + C.RESET + C.INFO + "." + C.RESET);
            return;
        }
        IO.println(C.INFO + "Rango sugerido: " + minActual + " a " + maxActual + "." + C.RESET);
    }*/

    static int leerInt(Scanner entrada, String texto) {
        while (true) {
            IO.print(texto);
            if (entrada.hasNextInt()) {
                int v = entrada.nextInt();
                entrada.nextLine();
                return v;
            }
            entrada.nextLine();
            IO.println(C.BAD + "Entrada inválida. Escribe un número entero." + C.RESET);
        }
    }

    static int leerIntRango(Scanner entrada, String texto) {
        while (true) {
            int v = leerInt(entrada, texto);
            if (v >= 1 && v <= 4) return v;
            IO.println(C.BAD + "Opción inválida." + C.RESET);
        }
    }

    static String leerLineaNoVacia(Scanner entrada, String texto) {
        while (true) {
            IO.print(texto);
            String s = entrada.nextLine();
            if (!s.trim().isEmpty()) return s.trim();
            IO.println(C.BAD + "Entrada inválida." + C.RESET);
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

    static void ordenarMarcador() {
        marcador.sort(
                Comparator.comparingDouble(Main::eficiencia)
                        .thenComparingInt(Score::intentos)
                        .thenComparingInt(s -> -((s.maximo() - s.minimo()) + 1))
        );
    }

    static void recortarMarcador() {
        while (marcador.size() > MAX_REGISTROS) marcador.removeLast();
    }

    static void cargarMarcador() {
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
                        marcador.add(new Score(nombre, intentos, maxIntentos, minimo, maximo));
                    }
                } else if (p.length == 4) {
                    String nombre = p[0].trim();
                    int intentos = Integer.parseInt(p[1].trim());
                    int minimo = Integer.parseInt(p[2].trim());
                    int maximo = Integer.parseInt(p[3].trim());
                    if (!nombre.isEmpty() && intentos > 0 && minimo < maximo) {
                        marcador.add(new Score(nombre, intentos, 0, minimo, maximo));
                    }
                }
            }
            ordenarMarcador();
            recortarMarcador();
        } catch (IOException | NumberFormatException e) {
            IO.println(C.BAD + "No se pudo leer el marcador." + C.RESET);
        }
    }

    static void guardarMarcador() {
        ordenarMarcador();
        recortarMarcador();

        ArrayList<String> lineas = new ArrayList<>();
        for (Score s : marcador) {
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
            IO.println(C.BAD + "No se pudo guardar el marcador." + C.RESET);
        }
    }

    static void mostrarMarcador() {
        ordenarMarcador();

        IO.println(C.TITLE + "=== MARCADOR (Top " + 5 + ") ===" + C.RESET);
        int limite = Math.min(5, marcador.size());

        for (int i = 0; i < limite; i++) {
            Score s = marcador.get(i);
            int rango = (s.maximo() - s.minimo()) + 1;
            String intentosTxt = (s.maxIntentos() > 0) ? (s.intentos() + "/" + s.maxIntentos()) : String.valueOf(s.intentos());
            String efTxt = String.format(Locale.ROOT, "%.3f", eficiencia(s));

            String medal = (i == 0) ? (C.BOLD + C.fg256(220) + "★" + C.RESET)
                    : (i == 1) ? (C.BOLD + C.fg256(250) + "★" + C.RESET)
                    : (i == 2) ? (C.BOLD + C.fg256(208) + "★" + C.RESET)
                    : " ";

            System.out.printf(Locale.ROOT,
                    "%s %s%d)%s %s | %sintentos%s %s | %srango%s %d (%d..%d) | %seficiencia%s %s%n",
                    medal,
                    C.ACCENT, i + 1, C.RESET,
                    C.INFO + s.nombre() + C.RESET,
                    C.MUTED, C.RESET, C.OK + intentosTxt + C.RESET,
                    C.MUTED, C.RESET, rango, s.minimo(), s.maximo(),
                    C.MUTED, C.RESET, C.ACCENT + efTxt + C.RESET
            );
        }

        if (marcador.isEmpty()) IO.println(C.WARN + "(vacío)" + C.RESET);

        IO.println(C.MUTED + "Archivo: " + SCORE_PATH.toAbsolutePath() + C.RESET);
    }

    static void mostrarEstadisticas() {
        ordenarMarcador();

        IO.println(C.TITLE + "=== ESTADÍSTICAS ===" + C.RESET);

        int n = marcador.size();
        IO.println(C.INFO + "Registros guardados: " + C.RESET + C.ACCENT + n + C.RESET);
        if (n == 0) return;

        Set<String> jugadores = new HashSet<>();
        for (Score s : marcador) jugadores.add(s.nombre());
        IO.println(C.INFO + "Jugadores únicos: " + C.RESET + C.ACCENT + jugadores.size() + C.RESET);

        ArrayList<Integer> intentosList = new ArrayList<>();
        for (Score s : marcador) intentosList.add(s.intentos());
        Collections.sort(intentosList);

        int minIntentos = intentosList.getFirst();
        int maxIntentos = intentosList.getLast();
        double promIntentos = promedioInt(intentosList);
        double medianaIntentos = medianaInt(intentosList);
        double desvIntentos = desviacionEstandarInt(intentosList, promIntentos);

        IO.println(C.INFO + "Intentos (mín..máx): " + C.RESET + C.ACCENT + minIntentos + C.RESET + C.MUTED + " .. " + C.RESET + C.ACCENT + maxIntentos + C.RESET);
        IO.println(String.format(Locale.ROOT, "%sIntentos (promedio): %s%.2f%s", C.INFO, C.ACCENT, promIntentos, C.RESET));
        IO.println(String.format(Locale.ROOT, "%sIntentos (mediana): %s%.2f%s", C.INFO, C.ACCENT, medianaIntentos, C.RESET));
        IO.println(String.format(Locale.ROOT, "%sIntentos (desv. estándar): %s%.2f%s", C.INFO, C.ACCENT, desvIntentos, C.RESET));

        double minEf = Double.POSITIVE_INFINITY, maxEf = Double.NEGATIVE_INFINITY, sumEf = 0.0;
        for (Score s : marcador) {
            double ef = eficiencia(s);
            minEf = Math.min(minEf, ef);
            maxEf = Math.max(maxEf, ef);
            sumEf += ef;
        }

        IO.println(String.format(Locale.ROOT, "%sEficiencia (mín..máx): %s%.3f%s%s .. %s%.3f%s",
                C.INFO, C.ACCENT, minEf, C.RESET, C.MUTED, C.ACCENT, maxEf, C.RESET));
        IO.println(String.format(Locale.ROOT, "%sEficiencia (promedio): %s%.3f%s", C.INFO, C.ACCENT, (sumEf / n), C.RESET));

        Score mejor = marcador.getFirst();
        Score peor = marcador.getLast();

        IO.println();
        IO.println(C.OK + "Mejor partida:" + C.RESET);
        imprimirDetalleScore(mejor);

        IO.println(C.BAD + "Peor partida:" + C.RESET);
        imprimirDetalleScore(peor);

        Map<Integer, Integer> freqRango = new HashMap<>();
        for (Score s : marcador) {
            int rango = (s.maximo() - s.minimo()) + 1;
            freqRango.put(rango, freqRango.getOrDefault(rango, 0) + 1);
        }

        IO.println();
        IO.println(C.INFO + "Rangos más usados (Top 10):" + C.RESET);
        imprimirTopMapa(freqRango);

        Map<String, Integer> partidasPorJugador = new HashMap<>();
        for (Score s : marcador) partidasPorJugador.put(s.nombre(), partidasPorJugador.getOrDefault(s.nombre(), 0) + 1);

        IO.println();
        IO.println(C.INFO + "Jugadores con más partidas (Top 10):" + C.RESET);
        imprimirTopMapa(partidasPorJugador);
    }

    static void imprimirDetalleScore(Score s) {
        int rango = (s.maximo() - s.minimo()) + 1;
        String intentosTxt = (s.maxIntentos() > 0) ? (s.intentos() + "/" + s.maxIntentos()) : String.valueOf(s.intentos());
        System.out.printf(Locale.ROOT,
                "%s- %sJugador:%s %s | %sIntentos:%s %s | %sRango:%s %d (%d..%d) | %sEficiencia:%s %.3f%s%n",
                C.MUTED,
                C.INFO, C.RESET, C.ACCENT + s.nombre() + C.RESET,
                C.INFO, C.RESET, C.ACCENT + intentosTxt + C.RESET,
                C.INFO, C.RESET, rango, s.minimo(), s.maximo(),
                C.INFO, C.RESET, eficiencia(s),
                C.RESET
        );
    }

    static void imprimirTopMapa(Map<?, Integer> mapa) {
        ArrayList<Map.Entry<?, Integer>> entries = new ArrayList<>(mapa.entrySet());
        entries.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        int lim = Math.min(10, entries.size());
        for (int i = 0; i < lim; i++) {
            var e = entries.get(i);
            System.out.println(C.ACCENT + (i + 1) + ")" + C.RESET + " " + C.INFO + e.getKey() + C.RESET + C.MUTED + " -> " + C.RESET + C.OK + e.getValue() + C.RESET);
        }
        if (entries.isEmpty()) System.out.println(C.WARN + "(sin datos)" + C.RESET);
    }

    static double promedioInt(List<Integer> xs) {
        long sum = 0;
        for (int v : xs) sum += v;
        return sum / (double) xs.size();
    }

    static double medianaInt(List<Integer> xsOrdenada) {
        int n = xsOrdenada.size();
        if (n % 2 == 1) return xsOrdenada.get(n / 2);
        return (xsOrdenada.get(n / 2 - 1) + xsOrdenada.get(n / 2)) / 2.0;
    }

    static double desviacionEstandarInt(List<Integer> xs, double media) {
        if (xs.size() <= 1) return 0.0;
        double sum = 0.0;
        for (int v : xs) {
            double d = v - media;
            sum += d * d;
        }
        return Math.sqrt(sum / xs.size());
    }
}
