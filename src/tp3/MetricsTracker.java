package tp3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gestor centralizado de métricas de rendimiento (tiempo y memoria) para el TP N° 3.
 * <p>
 * Registra cada operación ejecutada durante la sesión interactiva, calculando tiempos
 * con {@link System#nanoTime()} y estimando el consumo de memoria en el Heap de la JVM
 * mediante {@link Runtime#getRuntime()}.
 * </p>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class MetricsTracker {

    /**
     * Registro inmutable de una ejecución medida en la sesión.
     *
     * @param operation     Nombre descriptivo del algoritmo u operación.
     * @param elementsCount Cantidad de elementos procesados.
     * @param elapsedNanos  Tiempo transcurrido en nanosegundos.
     * @param memoryMB      Memoria utilizada en el Heap de la JVM al finalizar (MB).
     * @param notes         Detalles adicionales o complejidad teórica observada.
     */
    public record MetricEntry(
            String operation,
            long elementsCount,
            long elapsedNanos,
            double memoryMB,
            String notes
    ) {
        public double elapsedMillis() {
            return elapsedNanos / 1_000_000.0;
        }

        public double elapsedSeconds() {
            return elapsedNanos / 1_000_000_000.0;
        }
    }

    private final List<MetricEntry> history = new ArrayList<>();

    /**
     * Registra una nueva métrica de ejecución capturando automáticamente la memoria usada actual.
     *
     * @param operation     Nombre de la operación o algoritmo.
     * @param elementsCount Cantidad de elementos procesados.
     * @param elapsedNanos  Duración medida en nanosegundos.
     * @param notes         Notas adicionales (ej. estado, extrapolación o complejidad).
     */
    public void record(String operation, long elementsCount, long elapsedNanos, String notes) {
        double usedMB = getUsedMemoryMB();
        history.add(new MetricEntry(operation, elementsCount, elapsedNanos, usedMB, notes));
    }

    /**
     * Calcula la memoria actualmente utilizada en el Heap de la JVM en Megabytes (MB).
     *
     * @return Memoria usada en MB.
     */
    public static double getUsedMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        return usedBytes / (1024.0 * 1024.0);
    }

    /**
     * Calcula la memoria máxima disponible para el Heap de la JVM (`-Xmx`) en Megabytes (MB).
     *
     * @return Memoria máxima en MB.
     */
    public static double getMaxMemoryMB() {
        return Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
    }

    /**
     * Formatea una duración en nanosegundos mostrando milisegundos (ms) y segundos (s),
     * junto con la memoria usada actual del Heap.
     *
     * @param elapsedNanos Duración en nanosegundos.
     * @return Cadena formateada lista para consola.
     */
    public static String formatTimeAndMem(long elapsedNanos) {
        double ms = elapsedNanos / 1_000_000.0;
        double sec = elapsedNanos / 1_000_000_000.0;
        return String.format(Locale.US, "%.3f ms (%.4f s) | Memoria JVM usada: %.2f MB / Max: %.2f MB",
                ms, sec, getUsedMemoryMB(), getMaxMemoryMB());
    }

    /**
     * Formatea únicamente una duración en nanosegundos en milisegundos y segundos.
     *
     * @param elapsedNanos Duración en nanosegundos.
     * @return Cadena con formato "X.XXX ms (Y.YYYY s)".
     */
    public static String formatDuration(long elapsedNanos) {
        double ms = elapsedNanos / 1_000_000.0;
        double sec = elapsedNanos / 1_000_000_000.0;
        return String.format(Locale.US, "%.3f ms (%.4f s)", ms, sec);
    }

    /**
     * Imprime en consola una tabla consolidada con todas las métricas registradas en la sesión.
     */
    public void printSummaryTable() {
        System.out.println("\n=====================================================================================================================");
        System.out.println("                             RESUMEN CONSOLIDADO DE TIEMPOS Y MÉTRICAS DE LA SESIÓN                                  ");
        System.out.println("=====================================================================================================================");
        if (history.isEmpty()) {
            System.out.println("  [!] Aún no se han registrado operaciones en esta sesión.");
            System.out.println("=====================================================================================================================");
            return;
        }

        System.out.printf(Locale.US, "%-4s | %-34s | %14s | %14s | %11s | %9s | %-20s%n",
                "#", "Operación / Algoritmo", "Elementos (N)", "Tiempo (ms)", "Tiempo (s)", "RAM (MB)", "Detalle / Complejidad");
        System.out.println("-----+------------------------------------+----------------+----------------+-------------+-----------+----------------------");

        int idx = 1;
        for (MetricEntry entry : history) {
            System.out.printf(Locale.US, "%-4d | %-34s | %,14d | %14.3f | %11.4f | %9.2f | %-20s%n",
                    idx++,
                    truncate(entry.operation(), 34),
                    entry.elementsCount(),
                    entry.elapsedMillis(),
                    entry.elapsedSeconds(),
                    entry.memoryMB(),
                    entry.notes());
        }
        System.out.println("=====================================================================================================================");
        System.out.printf(Locale.US, " Estado actual JVM -> Memoria Usada: %.2f MB | Memoria Libre: %.2f MB | Memoria Máxima (-Xmx): %.2f MB%n",
                getUsedMemoryMB(),
                (Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())) / (1024.0 * 1024.0),
                getMaxMemoryMB());
        System.out.println("=====================================================================================================================");
    }

    /**
     * Obtiene la lista de métricas registradas.
     *
     * @return Lista inmutable de métricas.
     */
    public List<MetricEntry> getHistory() {
        return List.copyOf(history);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }
}

