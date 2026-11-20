package tp3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SplittableRandom;

/**
 * Utilidades de Entrada/Salida (I/O) de ultra alto rendimiento para procesar hasta
 * 100.000.000 de números enteros (~900 MB en disco) con asignación cero de objetos {@link String}.
 * <p>
 * <b>Decisiones de Ingeniería y Optimización de Memoria:</b>
 * <ul>
 *   <li>No se utiliza {@code java.util.Scanner} ni {@code BufferedReader.readLine()}, ya que instanciar
 *       100 millones de objetos {@code String} provocaría una presión insostenible sobre el Garbage Collector.</li>
 *   <li>Se emplean buffers directos de bytes ({@code byte[]} de 4 MB) sobre {@link BufferedInputStream} y
 *       {@link BufferedOutputStream}, convirtiendo dígitos ASCII a {@code int} y viceversa en memoria nativa.</li>
 *   <li>Para la generación pseudoaleatoria se utiliza {@link SplittableRandom}, que evita la contención atómica
 *       de {@code java.util.Random} y es significativamente más veloz.</li>
 * </ul>
 * </p>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class FastIO {

    /** Tamaño del buffer de lectura/escritura: 4 MB (4 * 1024 * 1024 bytes). */
    public static final int BUFFER_SIZE = 4 * 1024 * 1024;

    /** Cantidad por defecto de elementos según enunciado: 100.000.000. */
    public static final int DEFAULT_N = 100_000_000;

    /** Cota superior inclusiva del rango de valores: [0, 100.000.000]. */
    public static final int MAX_VALUE = 100_000_000;

    private FastIO() {
        // Clase utilitaria no instanciable
    }

    /**
     * Resultado de la búsqueda en streaming de mínimo y máximo.
     *
     * @param min          Valor mínimo encontrado.
     * @param max          Valor máximo encontrado.
     * @param count        Cantidad total de elementos leídos.
     * @param elapsedNanos Tiempo empleado en nanosegundos.
     */
    public record MinMaxResult(int min, int max, long count, long elapsedNanos) {}

    /**
     * Resultado de la verificación en streaming de ordenamiento ascendente.
     *
     * @param isSorted       {@code true} si todos los elementos cumplen {@code anterior <= actual}.
     * @param count          Cantidad total de elementos verificados.
     * @param errorIndex     Índice (base 0) donde se detectó la primera inversión, o -1 si está ordenado.
     * @param previousValue  Valor anterior en el punto de fallo.
     * @param currentValue   Valor actual menor que el anterior en el punto de fallo.
     * @param elapsedNanos   Tiempo empleado en nanosegundos.
     */
    public record VerificationResult(
            boolean isSorted,
            long count,
            long errorIndex,
            int previousValue,
            int currentValue,
            long elapsedNanos
    ) {}

    /**
     * Lector de enteros en streaming basado en bloques de 4 MB sobre {@code byte[]}.
     * Parsea enteros no negativos (y negativos si existieran) directamente desde bytes ASCII.
     */
    public static final class FastIntReader implements AutoCloseable {
        private final InputStream in;
        private final byte[] buffer;
        private int ptr = 0;
        private int len = 0;

        public FastIntReader(File file) throws IOException {
            this.in = new FileInputStream(file);
            this.buffer = new byte[BUFFER_SIZE];
        }

        private int readByte() throws IOException {
            if (ptr >= len) {
                len = in.read(buffer, 0, buffer.length);
                ptr = 0;
                if (len <= 0) {
                    return -1;
                }
            }
            return buffer[ptr++] & 0xFF;
        }

        /**
         * Lee el siguiente entero del archivo o devuelve {@code Integer.MIN_VALUE} al alcanzar EOF.
         *
         * @return El entero leído, o {@link Integer#MIN_VALUE} si no quedan más números.
         * @throws IOException Si ocurre un error de lectura.
         */
        public int nextIntOrEof() throws IOException {
            int b = readByte();
            while (b != -1 && b <= ' ') {
                b = readByte();
            }
            if (b == -1) {
                return Integer.MIN_VALUE;
            }
            int sign = 1;
            if (b == '-') {
                sign = -1;
                b = readByte();
            }
            int val = 0;
            while (b >= '0' && b <= '9') {
                val = val * 10 + (b - '0');
                b = readByte();
            }
            return val * sign;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /**
     * Escritor de enteros de alto rendimiento basado en un buffer propio de 4 MB ({@code byte[]}).
     * Convierte enteros a dígitos ASCII directamente en el buffer sin crear objetos {@link String}.
     */
    public static final class FastIntWriter implements AutoCloseable {
        private final OutputStream out;
        private final byte[] buffer;
        private final byte[] tempDigits = new byte[16];
        private int ptr = 0;

        public FastIntWriter(File file) throws IOException {
            this.out = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
            this.buffer = new byte[BUFFER_SIZE];
        }

        /**
         * Escribe un entero seguido de salto de línea ({@code '\n'}) en el buffer de salida.
         *
         * @param value Entero a escribir.
         * @throws IOException Si ocurre un error de escritura en disco.
         */
        public void writeIntLn(int value) throws IOException {
            if (ptr >= buffer.length - 16) {
                flushBuffer();
            }
            if (value == 0) {
                buffer[ptr++] = '0';
                buffer[ptr++] = '\n';
                return;
            }
            if (value < 0) {
                buffer[ptr++] = '-';
                value = -value;
            }
            int d = 0;
            while (value > 0) {
                int q = value / 10;
                tempDigits[d++] = (byte) ('0' + (value - q * 10));
                value = q;
            }
            while (d > 0) {
                buffer[ptr++] = tempDigits[--d];
            }
            buffer[ptr++] = '\n';
        }

        private void flushBuffer() throws IOException {
            if (ptr > 0) {
                out.write(buffer, 0, ptr);
                ptr = 0;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                flushBuffer();
                out.flush();
            } finally {
                out.close();
            }
        }
    }

    /**
     * Opción a): Genera el archivo {@code Datos.txt} con {@code count} enteros aleatorios uniformes
     * en el rango {@code [0, maxInclusive]}, escribiendo un número por línea.
     * <p>
     * Complejidad temporal: O(n). Complejidad espacial auxiliar: O(1) (buffer fijo de 4 MB).
     * </p>
     *
     * @param file         Archivo de destino (ej. {@code Datos.txt}).
     * @param count        Cantidad de números a generar (por defecto 100.000.000).
     * @param maxInclusive Valor máximo inclusivo del rango (por defecto 100.000.000).
     * @return Tiempo de ejecución en nanosegundos.
     * @throws IOException Si ocurre un fallo de escritura.
     */
    public static long generateDataFile(File file, int count, int maxInclusive) throws IOException {
        long start = System.nanoTime();
        SplittableRandom rng = new SplittableRandom();
        final int bound = maxInclusive + 1;
        final int progressStep = Math.max(1, count / 10);

        try (FastIntWriter writer = new FastIntWriter(file)) {
            for (int i = 1; i <= count; i++) {
                writer.writeIntLn(rng.nextInt(bound));
                if (count >= 10_000_000 && i % progressStep == 0) {
                    int pct = (int) ((i * 100L) / count);
                    System.out.printf("  -> Progreso generación: %3d%% (%,d / %,d elementos)%n", pct, i, count);
                }
            }
        }
        return System.nanoTime() - start;
    }

    /**
     * Opción b): Recorre el archivo en streaming (complejidad espacial O(1)) para encontrar
     * el valor mínimo, el valor máximo y contabilizar la cantidad total de elementos.
     *
     * @param file Archivo de entrada ({@code Datos.txt}).
     * @return {@link MinMaxResult} con mínimo, máximo, cantidad leída y tiempo en nanosegundos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static MinMaxResult findMinMaxStreaming(File file) throws IOException {
        long start = System.nanoTime();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long count = 0;

        try (FastIntReader reader = new FastIntReader(file)) {
            int val;
            while ((val = reader.nextIntOrEof()) != Integer.MIN_VALUE) {
                if (val < min) min = val;
                if (val > max) max = val;
                count++;
            }
        }
        long elapsed = System.nanoTime() - start;
        return new MinMaxResult(count > 0 ? min : 0, count > 0 ? max : 0, count, elapsed);
    }

    /**
     * Cuenta la cantidad rápida de líneas/enteros en un archivo mediante streaming de bytes.
     *
     * @param file Archivo a inspeccionar.
     * @return Cantidad total de enteros presentes en el archivo.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static int countIntsInFile(File file) throws IOException {
        int count = 0;
        try (FastIntReader reader = new FastIntReader(file)) {
            while (reader.nextIntOrEof() != Integer.MIN_VALUE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Carga hasta {@code maxElements} enteros desde un archivo hacia un array primitivo {@code int[]}.
     * <p>
     * Si se conoce la cantidad exacta o límite, reserva exactamente un único {@code int[]} evitando
     * realocaciones que duplicarían el consumo de memoria RAM.
     * </p>
     *
     * @param file        Archivo de origen ({@code Datos.txt}).
     * @param maxElements Cantidad máxima de elementos a cargar (o {@code <= 0} para cargar todos).
     * @return Array primitivo {@code int[]} con los datos leídos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static int[] readIntsToArray(File file, int maxElements) throws IOException {
        int targetSize;
        if (maxElements > 0) {
            // Contamos o acotamos sin desperdiciar RAM si el archivo es más pequeño que maxElements
            int totalInFile = countIntsInFile(file);
            targetSize = Math.min(maxElements, totalInFile);
        } else {
            targetSize = countIntsInFile(file);
        }

        int[] data = new int[targetSize];
        int idx = 0;
        try (FastIntReader reader = new FastIntReader(file)) {
            int val;
            while (idx < targetSize && (val = reader.nextIntOrEof()) != Integer.MIN_VALUE) {
                data[idx++] = val;
            }
        }
        return data;
    }

    /**
     * Escribe el contenido de un array primitivo {@code int[]} (hasta {@code length} elementos)
     * en un archivo de texto, un entero por línea, utilizando {@link FastIntWriter}.
     *
     * @param file   Archivo de destino.
     * @param data   Array primitivo de enteros.
     * @param length Cantidad de elementos del array a escribir.
     * @return Tiempo empleado en la escritura en nanosegundos.
     * @throws IOException Si ocurre un error de escritura.
     */
    public static long writeArrayToFile(File file, int[] data, int length) throws IOException {
        long start = System.nanoTime();
        try (FastIntWriter writer = new FastIntWriter(file)) {
            for (int i = 0; i < length; i++) {
                writer.writeIntLn(data[i]);
            }
        }
        return System.nanoTime() - start;
    }

    /**
     * Opción c): Construye el arreglo de frecuencias {@code int[maxValue + 1]} leyendo el archivo
     * en streaming. Permite procesar 100.000.000 de enteros usando únicamente ~400 MB para el vector
     * de frecuencias, sin almacenar los 100M de números de entrada en memoria.
     *
     * @param file     Archivo de entrada ({@code Datos.txt}).
     * @param freq     Array de frecuencias preasignado de tamaño {@code maxValue + 1}.
     * @return Cantidad total de elementos leídos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static long accumulateFrequencies(File file, int[] freq) throws IOException {
        long count = 0;
        int maxAllowed = freq.length - 1;
        try (FastIntReader reader = new FastIntReader(file)) {
            int val;
            while ((val = reader.nextIntOrEof()) != Integer.MIN_VALUE) {
                if (val >= 0 && val <= maxAllowed) {
                    freq[val]++;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Opción f): Verifica en streaming (memoria auxiliar O(1)) que un archivo de salida esté
     * ordenado de forma ascendente ({@code anterior <= actual} para todo par consecutivo).
     *
     * @param file Archivo a verificar (ej. {@code s_CountingSort.txt}, {@code s_SelectionSort.txt}, {@code s_HeapSort.txt}).
     * @return {@link VerificationResult} con el estado de la verificación, cantidad de elementos y tiempo.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static VerificationResult verifySortedStreaming(File file) throws IOException {
        long start = System.nanoTime();
        long count = 0;
        int prev = Integer.MIN_VALUE;

        try (FastIntReader reader = new FastIntReader(file)) {
            int current;
            while ((current = reader.nextIntOrEof()) != Integer.MIN_VALUE) {
                if (count > 0 && current < prev) {
                    long elapsed = System.nanoTime() - start;
                    return new VerificationResult(false, count + 1, count, prev, current, elapsed);
                }
                prev = current;
                count++;
            }
        }
        long elapsed = System.nanoTime() - start;
        return new VerificationResult(true, count, -1, 0, 0, elapsed);
    }
}

