package tp3;

import java.io.File;
import java.io.IOException;

/**
 * Implementación de algoritmos de análisis de frecuencias y ordenamiento para el TP N° 3:
 * <ul>
 *   <li><b>Top-10 Frecuencias:</b> Búsqueda en O(n + k) mediante arreglo de conteo directo.</li>
 *   <li><b>Counting Sort:</b> Algoritmo no comparativo en O(n + k) por distribución/conteo.</li>
 *   <li><b>Selection Sort:</b> Algoritmo por Fuerza Bruta en O(n²) con soporte de muestreo y extrapolación.</li>
 *   <li><b>Heap Sort:</b> Algoritmo del paradigma <i>Transformar y Conquistar</i> (cambio de representación
 *       a Max-Heap in-place con construcción de Floyd O(n) y extracción sucesiva en O(n log n)).</li>
 * </ul>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class SortingAlgorithms {

    private SortingAlgorithms() {
        // Clase utilitaria no instanciable
    }

    /**
     * Par (valor, frecuencia) para el reporte de los 10 números más repetidos.
     *
     * @param value     Número entero en el rango [0, 100.000.000].
     * @param frequency Cantidad exacta de apariciones en el archivo.
     */
    public record FrequencyItem(int value, int frequency) {}

    /**
     * Resultado del análisis de frecuencias Top-K.
     *
     * @param topItems      Arreglo ordenado de mayor a menor frecuencia con los Top-K valores.
     * @param totalElements Cantidad total de números procesados.
     * @param elapsedNanos  Tiempo total de ejecución en nanosegundos.
     */
    public record TopKFrequencyResult(FrequencyItem[] topItems, long totalElements, long elapsedNanos) {}

    /**
     * Resultado desglosado de la ejecución de Counting Sort.
     *
     * @param totalElements  Cantidad total de elementos ordenados.
     * @param readCountNanos Tiempo de lectura en streaming y conteo de frecuencias (ns).
     * @param writeNanos     Tiempo de emisión ordenada al archivo de salida (ns).
     * @param totalNanos     Tiempo total de la operación (ns).
     */
    public record CountingSortResult(long totalElements, long readCountNanos, long writeNanos, long totalNanos) {}

    /**
     * Resultado desglosado de la ejecución de Selection Sort.
     *
     * @param sampleSize        Tamaño de la muestra efectivamente ordenada (M).
     * @param totalElementsFile Tamaño total del archivo original (N).
     * @param readNanos         Tiempo de lectura de los M elementos (ns).
     * @param sortNanos         Tiempo puro de ordenamiento O(M²) (ns).
     * @param writeNanos        Tiempo de escritura en {@code s_SelectionSort.txt} (ns).
     * @param extrapolatedSeconds Estimación teórica T(N) ≈ T(M) * (N / M)² en segundos.
     */
    public record SelectionSortResult(
            int sampleSize,
            int totalElementsFile,
            long readNanos,
            long sortNanos,
            long writeNanos,
            double extrapolatedSeconds
    ) {}

    /**
     * Resultado desglosado de la ejecución de Heap Sort.
     *
     * @param totalElements  Cantidad de elementos ordenados.
     * @param readNanos      Tiempo de carga del archivo en {@code int[]} (ns).
     * @param buildHeapNanos Tiempo de construcción del Max-Heap mediante algoritmo de Floyd O(n) (ns).
     * @param sortNanos      Tiempo de extracción y reordenamiento in-place O(n log n) (ns).
     * @param writeNanos     Tiempo de escritura a {@code s_HeapSort.txt} (ns).
     * @param verification   Resultado de la verificación automática en streaming del archivo generado.
     */
    public record HeapSortResult(
            int totalElements,
            long readNanos,
            long buildHeapNanos,
            long sortNanos,
            long writeNanos,
            FastIO.VerificationResult verification
    ) {}

    /**
     * Opción c): Obtiene los {@code topK} números que más se repiten en {@code Datos.txt}.
     * <p>
     * <b>Complejidad Temporal:</b> O(n + k), donde n = cantidad de datos (10^8) y k = rango de valores (10^8).
     * <br><b>Complejidad Espacial:</b> O(k) = {@code int[100_000_001]} (~400 MB en RAM).
     * </p>
     *
     * @param inputFile Archivo {@code Datos.txt}.
     * @param maxValue  Valor máximo posible del dominio (100.000.000).
     * @param topK      Cantidad de elementos más frecuentes a recuperar (10).
     * @return {@link TopKFrequencyResult} con la tabla Top-10 y tiempos medidos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public static TopKFrequencyResult findTopKFrequent(File inputFile, int maxValue, int topK) throws IOException {
        long start = System.nanoTime();
        int[] freq = new int[maxValue + 1];
        long totalElements = FastIO.accumulateFrequencies(inputFile, freq);

        int[] bestValues = new int[topK];
        int[] bestCounts = new int[topK];

        // Recorrido lineal O(k) manteniendo una tabla ordenada de tamaño topK (10) por inserción
        for (int val = 0; val <= maxValue; val++) {
            int c = freq[val];
            if (c > bestCounts[topK - 1]) {
                int pos = topK - 1;
                while (pos > 0 && c > bestCounts[pos - 1]) {
                    bestCounts[pos] = bestCounts[pos - 1];
                    bestValues[pos] = bestValues[pos - 1];
                    pos--;
                }
                bestCounts[pos] = c;
                bestValues[pos] = val;
            }
        }

        int actualTop = 0;
        for (int i = 0; i < topK; i++) {
            if (bestCounts[i] > 0) {
                actualTop++;
            }
        }

        FrequencyItem[] items = new FrequencyItem[actualTop];
        for (int i = 0; i < actualTop; i++) {
            items[i] = new FrequencyItem(bestValues[i], bestCounts[i]);
        }

        long elapsed = System.nanoTime() - start;
        return new TopKFrequencyResult(items, totalElements, elapsed);
    }

    /**
     * Opción d): Ordena los datos de {@code inputFile} mediante <b>Counting Sort</b> y escribe el
     * resultado ascendente en {@code outputFile} ({@code s_CountingSort.txt}).
     * <p>
     * <b>Fundamento Teórico y Optimización de Espacio en Java:</b>
     * <ul>
     *   <li><b>Counting Sort Estable Clásico (con sumas prefijas acumuladas):</b>
     *       Cuando se ordenan registros compuestos asociados a una clave entera en {@code [0, k]}, el
     *       algoritmo clásico requiere 3 arreglos: el arreglo de entrada {@code A[0..n-1]}, el arreglo de
     *       conteos/sumas acumuladas {@code C[0..k]} donde {@code C[i] = C[i] + C[i-1]}, y el arreglo de
     *       salida {@code B[0..n-1]} rellenado de derecha a izquierda ({@code B[--C[A[j]]] = A[j]}) para
     *       preservar la estabilidad. Para {@code n = 100.000.000} y {@code k = 100.000.000}, eso exigiría
     *       3 arreglos de 400 MB cada uno (~1.2 GB simultáneos en RAM).</li>
     *   <li><b>Optimización Crítica para Enteros Puros:</b>
     *       Dado que los elementos son números enteros atómicos sin datos satélites distinguibles, dos
     *       apariciones del mismo entero son idénticas en valor. Por lo tanto, acumulamos directamente las
     *       frecuencias {@code freq[v]} leyendo {@code Datos.txt} en streaming y emitimos {@code freq[v]}
     *       veces cada valor {@code v} desde {@code 0} hasta {@code k} hacia el buffer de salida. Esto
     *       reduce el consumo de memoria de ~1.2 GB a solo ~400 MB y evita picos de Garbage Collection.</li>
     * </ul>
     * <b>Complejidad Temporal:</b> Θ(n + k). <b>Complejidad Espacial:</b> Θ(k).
     * </p>
     *
     * @param inputFile  Archivo de entrada ({@code Datos.txt}).
     * @param outputFile Archivo de salida ({@code s_CountingSort.txt}).
     * @param maxValue   Cota superior inclusiva del dominio (100.000.000).
     * @return {@link CountingSortResult} con los tiempos desglosados de lectura/conteo y escritura.
     * @throws IOException Si ocurre un error de lectura o escritura.
     */
    public static CountingSortResult countingSortToFile(File inputFile, File outputFile, int maxValue) throws IOException {
        long startTotal = System.nanoTime();

        long startRead = System.nanoTime();
        int[] freq = new int[maxValue + 1];
        long totalElements = FastIO.accumulateFrequencies(inputFile, freq);
        long readCountNanos = System.nanoTime() - startRead;

        long startWrite = System.nanoTime();
        try (FastIO.FastIntWriter writer = new FastIO.FastIntWriter(outputFile)) {
            for (int val = 0; val <= maxValue; val++) {
                int occurrences = freq[val];
                for (int c = 0; c < occurrences; c++) {
                    writer.writeIntLn(val);
                }
            }
        }
        long writeNanos = System.nanoTime() - startWrite;
        long totalNanos = System.nanoTime() - startTotal;

        return new CountingSortResult(totalElements, readCountNanos, writeNanos, totalNanos);
    }

    /**
     * Versión en memoria del Counting Sort estable clásico con sumas acumuladas (prefijos).
     * <p>
     * Se incluye como referencia algorítmica documentada del método estable canónico:
     * <ol>
     *   <li>Contar frecuencias de cada clave en {@code count[0..k]}.</li>
     *   <li>Transformar {@code count} en sumas acumuladas: {@code count[i] += count[i - 1]}.</li>
     *   <li>Recorrer el arreglo original de derecha a izquierda ubicando cada elemento en su posición final.</li>
     * </ol>
     * </p>
     *
     * @param input    Arreglo de enteros no negativos a ordenar.
     * @param maxValue Valor máximo presente en {@code input}.
     * @return Nuevo arreglo ordenado de manera estable.
     */
    public static int[] classicStableCountingSortInMemory(int[] input, int maxValue) {
        int n = input.length;
        int[] count = new int[maxValue + 1];
        int[] output = new int[n];

        for (int i = 0; i < n; i++) {
            count[input[i]]++;
        }
        for (int i = 1; i <= maxValue; i++) {
            count[i] += count[i - 1];
        }
        for (int i = n - 1; i >= 0; i--) {
            int val = input[i];
            output[--count[val]] = val;
        }
        return output;
    }

    /**
     * Opción e): Ordena un arreglo primitivo {@code int[]} in-place mediante <b>Selection Sort</b>.
     * <p>
     * <b>Complejidad Temporal:</b> Θ(n²) comparaciones en todos los casos (Mejor, Promedio y Peor),
     * realizando exactamente {@code n * (n - 1) / 2} comparaciones y a lo sumo {@code n - 1} intercambios.
     * <br><b>Complejidad Espacial Auxiliar:</b> O(1).
     * </p>
     *
     * @param arr Arreglo primitivo a ordenar in-place.
     * @return Tiempo puro de ordenamiento en nanosegundos.
     */
    public static long selectionSortInPlace(int[] arr) {
        long start = System.nanoTime();
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            int minVal = arr[i];
            for (int j = i + 1; j < n; j++) {
                int candidate = arr[j];
                if (candidate < minVal) {
                    minVal = candidate;
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                arr[minIdx] = arr[i];
                arr[i] = minVal;
            }
        }
        return System.nanoTime() - start;
    }

    /**
     * Ejecuta el flujo completo de Selection Sort sobre {@code sampleSize} elementos de {@code inputFile},
     * guarda la salida en {@code outputFile} ({@code s_SelectionSort.txt}) y calcula la extrapolación
     * cuadrática {@code T(N) ≈ T(M) * (N / M)²} para el tamaño total del archivo.
     *
     * @param inputFile         Archivo de entrada ({@code Datos.txt}).
     * @param outputFile        Archivo de salida ({@code s_SelectionSort.txt}).
     * @param sampleSize        Cantidad de elementos a ordenar (M).
     * @param totalElementsFile Cantidad total de elementos en el archivo original (N).
     * @return {@link SelectionSortResult} con tiempos reales y tiempo extrapolado para N completo.
     * @throws IOException Si ocurre un error de E/S.
     */
    public static SelectionSortResult selectionSortFile(
            File inputFile,
            File outputFile,
            int sampleSize,
            int totalElementsFile
    ) throws IOException {
        long startRead = System.nanoTime();
        int[] data = FastIO.readIntsToArray(inputFile, sampleSize);
        long readNanos = System.nanoTime() - startRead;

        int actualM = data.length;
        long sortNanos = selectionSortInPlace(data);
        long writeNanos = FastIO.writeArrayToFile(outputFile, data, actualM);

        double ratio = actualM > 0 ? ((double) totalElementsFile / actualM) : 1.0;
        double sortSeconds = sortNanos / 1_000_000_000.0;
        double extrapolatedSeconds = sortSeconds * (ratio * ratio);

        return new SelectionSortResult(actualM, totalElementsFile, readNanos, sortNanos, writeNanos, extrapolatedSeconds);
    }

    /**
     * Opción k): Ordena todos los datos de {@code inputFile} mediante <b>Heap Sort</b> in-place
     * y escribe el resultado ascendente en {@code outputFile} ({@code s_HeapSort.txt}), verificando
     * automáticamente en streaming que el archivo resultante esté ordenado.
     * <p>
     * <b>Paradigma Transformar y Conquistar (Cambio de Representación):</b>
     * <ol>
     *   <li><b>Fase 1 - Construcción del Max-Heap (Algoritmo bottom-up de Floyd):</b>
     *       Transforma el arreglo desordenado en un Max-Heap in-place aplicando {@code siftDown} desde
     *       el último nodo interno {@code (n / 2) - 1} hasta la raíz {@code 0}. Complejidad: Θ(n).</li>
     *   <li><b>Fase 2 - Extracción sucesiva del máximo:</b>
     *       Intercambia la raíz {@code arr[0]} (el máximo actual) con la última hoja activa {@code arr[end]},
     *       reduce el tamaño del heap en 1 y restaura la propiedad de Max-Heap en O(log n).
     *       Complejidad total: O(n log n) en Mejor, Promedio y Peor caso.</li>
     * </ol>
     * <b>Complejidad Espacial Auxiliar:</b> O(1) (opera estrictamente in-place sobre el {@code int[]} cargado).
     * </p>
     *
     * @param inputFile  Archivo de entrada ({@code Datos.txt}).
     * @param outputFile Archivo de salida ({@code s_HeapSort.txt}).
     * @return {@link HeapSortResult} con tiempos desglosados y resultado de verificación.
     * @throws IOException Si ocurre un error de lectura o escritura.
     */
    public static HeapSortResult heapSortFile(File inputFile, File outputFile) throws IOException {
        long startRead = System.nanoTime();
        int[] data = FastIO.readIntsToArray(inputFile, -1);
        long readNanos = System.nanoTime() - startRead;

        int n = data.length;

        // Fase 1: Construcción bottom-up del Max-Heap en O(n)
        long startBuild = System.nanoTime();
        for (int i = (n >>> 1) - 1; i >= 0; i--) {
            BinaryHeap.siftDownMax(data, i, n);
        }
        long buildHeapNanos = System.nanoTime() - startBuild;

        // Fase 2: Extracción del máximo y hundimiento (siftDown) en O(n log n)
        long startSort = System.nanoTime();
        for (int end = n - 1; end > 0; end--) {
            int maxVal = data[0];
            data[0] = data[end];
            data[end] = maxVal;
            BinaryHeap.siftDownMax(data, 0, end);
        }
        long sortNanos = System.nanoTime() - startSort;

        // Liberar o escribir directamente a disco
        long writeNanos = FastIO.writeArrayToFile(outputFile, data, n);

        // Permitir que el GC reclame el arreglo de 400 MB antes de verificar si fuera necesario
        data = null;

        FastIO.VerificationResult verification = FastIO.verifySortedStreaming(outputFile);

        return new HeapSortResult(n, readNanos, buildHeapNanos, sortNanos, writeNanos, verification);
    }
}

