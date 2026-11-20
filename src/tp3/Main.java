package tp3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Clase principal y orquestador interactivo por consola para el
 * <b>Trabajo Práctico N° 3 ("Transformar y Conquistar")</b> - Análisis y Diseño de Algoritmos (UNViMe).
 * <p>
 * Presenta el menú principal con las opciones {@code a)} a {@code k)} (y submenús especializados para
 * Árbol AVL y Heap Binario), validando entradas del usuario y registrando todas las métricas de
 * tiempo y consumo de memoria en {@link MetricsTracker}.
 * </p>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class Main {

    /** Archivo principal de entrada de datos. */
    public static final String DATA_FILE_NAME = "Datos.txt";

    /** Archivo de salida de Counting Sort. */
    public static final String COUNTING_SORT_FILE = "s_CountingSort.txt";

    /** Archivo de salida de Selection Sort. */
    public static final String SELECTION_SORT_FILE = "s_SelectionSort.txt";

    /** Archivo de salida de Heap Sort. */
    public static final String HEAP_SORT_FILE = "s_HeapSort.txt";

    private final BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
    private final MetricsTracker metrics = new MetricsTracker();

    // Instancias persistentes durante la sesión para permitir operaciones incrementales en submenús
    private AvlTree avlTree = new AvlTree(1024);
    private BinaryHeap maxHeap = new BinaryHeap(true, 1024);
    private BinaryHeap minHeap = new BinaryHeap(false, 1024);

    public static void main(String[] args) {
        new Main().runInteractiveLoop();
    }

    private void runInteractiveLoop() {
        printBanner();
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = readLinePrompt("Seleccione una opción (a-k, o 'q' para salir): ").trim().toLowerCase(Locale.ROOT);
            if (choice.isEmpty()) {
                continue;
            }

            try {
                switch (choice) {
                    case "a" -> handleOptionA_GenerateData();
                    case "b" -> handleOptionB_FindMinMax();
                    case "c" -> handleOptionC_Top10Frequency();
                    case "d" -> handleOptionD_CountingSort();
                    case "e" -> handleOptionE_SelectionSort();
                    case "f" -> handleOptionF_VerifySortings();
                    case "g" -> handleOptionG_SummaryTable();
                    case "h" -> handleOptionH_DocumentationInfo();
                    case "i" -> handleOptionI_AvlSubmenu();
                    case "j" -> handleOptionJ_HeapSubmenu();
                    case "k" -> handleOptionK_HeapSort();
                    case "q", "salir", "exit", "0" -> {
                        System.out.println("\n[+] Finalizando sesión. ¡Hasta luego!");
                        running = false;
                    }
                    default -> System.out.println("  [!] Opción no reconocida. Por favor ingrese una letra entre 'a' y 'k' (o 'q' para salir).");
                }
            } catch (OutOfMemoryError oom) {
                // Liberar estructuras pesadas si ocurre falta de heap
                System.gc();
                System.err.println("\n  [ERROR CRÍTICO DE MEMORIA - OutOfMemoryError]");
                System.err.printf(Locale.US, "  La JVM actual tiene un límite máximo (-Xmx) de %.2f MB.%n", MetricsTracker.getMaxMemoryMB());
                System.err.println("  Para procesar 100.000.000 de enteros en memoria, ejecute con al menos 4 GB - 6 GB de Heap:");
                System.err.println("    java -Xms2g -Xmx6g -cp bin tp3.Main");
            } catch (Exception ex) {
                System.err.println("\n  [!] Error durante la ejecución de la operación: " + ex.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("==============================================================================================");
        System.out.println("         UNIVERSIDAD NACIONAL DE VILLA MERCEDES (UNViMe) - INGENIERÍA / SISTEMAS              ");
        System.out.println("        ANÁLISIS Y DISEÑO DE ALGORITMOS - TRABAJO PRÁCTICO N° 3: TRANSFORMAR Y CONQUISTAR     ");
        System.out.println("==============================================================================================");
        System.out.printf(Locale.US, "  Entorno Java: JDK %s | Memoria Máxima Heap (-Xmx): %.2f MB%n",
                System.getProperty("java.version"), MetricsTracker.getMaxMemoryMB());
        System.out.println("==============================================================================================");
    }

    private void printMainMenu() {
        File dataFile = new File(DATA_FILE_NAME);
        String fileStatus = dataFile.exists()
                ? String.format(Locale.US, "Disponible (%.2f MB)", dataFile.length() / (1024.0 * 1024.0))
                : "No generado aún";

        System.out.println("\n-------------------------------------- MENÚ PRINCIPAL --------------------------------------");
        System.out.println("  Estado de " + DATA_FILE_NAME + ": " + fileStatus);
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("  a) Generación del archivo de datos (Datos.txt - hasta 100.000.000 de enteros)");
        System.out.println("  b) Búsqueda del máximo y mínimo (Streaming O(n) tiempo, O(1) memoria)");
        System.out.println("  c) Análisis de frecuencia (Top 10 números que más se repiten)");
        System.out.println("  d) Ordenamiento mediante Counting Sort -> s_CountingSort.txt (O(n + k))");
        System.out.println("  e) Ordenamiento mediante Selection Sort -> s_SelectionSort.txt (O(n²) con extrapolación)");
        System.out.println("  f) Verificación del ordenamiento en streaming (s_CountingSort, s_SelectionSort, s_HeapSort)");
        System.out.println("  g) Resumen de tiempos y métricas registradas en la sesión");
        System.out.println("  h) Información sobre documentación técnica y decisiones de arquitectura Java");
        System.out.println("  i) Submenú Árbol AVL (Transformar y Conquistar - Arrays Paralelos)");
        System.out.println("  j) Submenú Heap Binario (Max-Heap y Min-Heap sobre int[])");
        System.out.println("  k) Ordenamiento mediante Heap Sort -> s_HeapSort.txt (O(n log n) in-place)");
        System.out.println("  q) Salir del programa");
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    /**
     * Opción a): Generación de {@code Datos.txt}.
     */
    private void handleOptionA_GenerateData() throws IOException {
        System.out.println("\n>>> [Opción a] Generación del archivo de datos (" + DATA_FILE_NAME + ")");
        System.out.printf(Locale.US, "  Valor por defecto: %,d enteros aleatorios uniformes en [0, %,d].%n",
                FastIO.DEFAULT_N, FastIO.MAX_VALUE);
        int n = readIntWithDefault("  Ingrese cantidad N a generar [Enter = 100.000.000]: ", FastIO.DEFAULT_N, 1, Integer.MAX_VALUE);

        File file = new File(DATA_FILE_NAME);
        System.out.printf(Locale.US, "  Generando %,d números en '%s' usando SplittableRandom y buffer de 4 MB...%n", n, file.getName());

        long elapsed = FastIO.generateDataFile(file, n, FastIO.MAX_VALUE);
        double sizeMB = file.length() / (1024.0 * 1024.0);

        System.out.println("  [OK] Archivo generado exitosamente.");
        System.out.printf(Locale.US, "  -> Cantidad de elementos : %,d%n", n);
        System.out.printf(Locale.US, "  -> Tamaño en disco       : %.2f MB (%,d bytes)%n", sizeMB, file.length());
        System.out.printf(Locale.US, "  -> Tiempo de ejecución   : %s%n", MetricsTracker.formatTimeAndMem(elapsed));

        metrics.record("a) Generación Datos.txt", n, elapsed, String.format(Locale.US, "%.2f MB en disco", sizeMB));
    }

    /**
     * Opción b): Búsqueda del máximo y mínimo en streaming.
     */
    private void handleOptionB_FindMinMax() throws IOException {
        File file = requireDataFile();
        if (file == null) return;

        System.out.println("\n>>> [Opción b] Búsqueda del Máximo y Mínimo en streaming (Memoria Auxiliar O(1))");
        FastIO.MinMaxResult res = FastIO.findMinMaxStreaming(file);

        System.out.printf(Locale.US, "  -> Elementos leídos      : %,d%n", res.count());
        System.out.printf(Locale.US, "  -> Valor Mínimo          : %,d%n", res.min());
        System.out.printf(Locale.US, "  -> Valor Máximo          : %,d%n", res.max());
        System.out.printf(Locale.US, "  -> Tiempo de ejecución   : %s%n", MetricsTracker.formatTimeAndMem(res.elapsedNanos()));

        metrics.record("b) Búsqueda Máximo y Mínimo", res.count(), res.elapsedNanos(),
                String.format(Locale.US, "Min=%,d | Max=%,d", res.min(), res.max()));
    }

    /**
     * Opción c): Análisis de frecuencia (Top 10).
     */
    private void handleOptionC_Top10Frequency() throws IOException {
        File file = requireDataFile();
        if (file == null) return;

        System.out.println("\n>>> [Opción c] Análisis de Frecuencia: Top 10 números que más se repiten (O(n + k))");
        if (!checkMemoryFor400MBArray("Arreglo de frecuencias int[100.000.001]")) {
            return;
        }

        SortingAlgorithms.TopKFrequencyResult res = SortingAlgorithms.findTopKFrequent(file, FastIO.MAX_VALUE, 10);

        System.out.println("\n  +---------+--------------------+--------------------+------------------+");
        System.out.println("  | Puesto  | Número             | Apariciones (Frec) | % sobre el Total |");
        System.out.println("  +---------+--------------------+--------------------+------------------+");
        int rank = 1;
        for (SortingAlgorithms.FrequencyItem item : res.topItems()) {
            double pct = res.totalElements() > 0 ? (item.frequency() * 100.0) / res.totalElements() : 0.0;
            System.out.printf(Locale.US, "  | #%2d     | %,18d | %,18d | %15.6f%% |%n",
                    rank++, item.value(), item.frequency(), pct);
        }
        System.out.println("  +---------+--------------------+--------------------+------------------+");
        System.out.printf(Locale.US, "  -> Total elementos analizados: %,d%n", res.totalElements());
        System.out.printf(Locale.US, "  -> Tiempo total              : %s%n", MetricsTracker.formatTimeAndMem(res.elapsedNanos()));

        String top1Note = res.topItems().length > 0
                ? String.format(Locale.US, "Top1=%,d (%d veces)", res.topItems()[0].value(), res.topItems()[0].frequency())
                : "Sin datos";
        metrics.record("c) Frecuencia Top 10", res.totalElements(), res.elapsedNanos(), top1Note);
    }

    /**
     * Opción d): Ordenamiento mediante Counting Sort.
     */
    private void handleOptionD_CountingSort() throws IOException {
        File file = requireDataFile();
        if (file == null) return;

        System.out.println("\n>>> [Opción d] Ordenamiento mediante Counting Sort -> " + COUNTING_SORT_FILE);
        if (!checkMemoryFor400MBArray("Vector de frecuencias Counting Sort int[100.000.001]")) {
            return;
        }

        File outFile = new File(COUNTING_SORT_FILE);
        SortingAlgorithms.CountingSortResult res = SortingAlgorithms.countingSortToFile(file, outFile, FastIO.MAX_VALUE);

        System.out.println("  [OK] Ordenamiento por Counting Sort finalizado.");
        System.out.printf(Locale.US, "  -> Elementos ordenados       : %,d%n", res.totalElements());
        System.out.printf(Locale.US, "  -> Tiempo Lectura + Conteo   : %s%n", MetricsTracker.formatDuration(res.readCountNanos()));
        System.out.printf(Locale.US, "  -> Tiempo Escritura Ordenada : %s%n", MetricsTracker.formatDuration(res.writeNanos()));
        System.out.printf(Locale.US, "  -> Tiempo Total              : %s%n", MetricsTracker.formatTimeAndMem(res.totalNanos()));

        metrics.record("d) Counting Sort", res.totalElements(), res.totalNanos(), "O(n + k) -> " + COUNTING_SORT_FILE);
    }

    /**
     * Opción e): Ordenamiento mediante Selection Sort con protección de cuello de botella O(n²) y extrapolación.
     */
    private void handleOptionE_SelectionSort() throws IOException {
        File file = requireDataFile();
        if (file == null) return;

        System.out.println("\n>>> [Opción e] Ordenamiento mediante Selection Sort -> " + SELECTION_SORT_FILE);
        int totalElements = FastIO.countIntsInFile(file);
        int sampleSize = totalElements;

        if (totalElements > 100_000) {
            double approxComparisons = ((double) totalElements * (totalElements - 1)) / 2.0;
            System.out.println("  +---------------------------------------------------------------------------------------+");
            System.out.println("  | [ADVERTENCIA DE COMPLEJIDAD CUADRÁTICA O(n²)]                                         |");
            System.out.printf(Locale.US, "  | El archivo '%s' contiene %,d elementos.%n", file.getName(), totalElements);
            System.out.printf(Locale.US, "  | Ejecutar Selection Sort sobre el total requiere ~%.2e comparaciones,%n", approxComparisons);
            System.out.println("  | lo cual demoraría semanas o meses de CPU continua.                                    |");
            System.out.println("  +---------------------------------------------------------------------------------------+");
            System.out.println("  Puede elegir un tamaño de muestra M (por defecto 50.000) para medir el tiempo real");
            System.out.println("  y calcular la extrapolación matemática T(N) ≈ T(M) * (N/M)², o ingresar " + totalElements + " para forzar el total.");

            sampleSize = readIntWithDefault("  Ingrese tamaño de muestra M [Enter = 50.000]: ", 50_000, 1, totalElements);
        }

        System.out.printf(Locale.US, "  Ordenando M = %,d elementos con Selection Sort O(M²)...%n", sampleSize);
        File outFile = new File(SELECTION_SORT_FILE);
        SortingAlgorithms.SelectionSortResult res = SortingAlgorithms.selectionSortFile(file, outFile, sampleSize, totalElements);

        long totalNanos = res.readNanos() + res.sortNanos() + res.writeNanos();
        System.out.println("  [OK] Selection Sort finalizado y guardado en '" + SELECTION_SORT_FILE + "'.");
        System.out.printf(Locale.US, "  -> Muestra ordenada (M)      : %,d elementos%n", res.sampleSize());
        System.out.printf(Locale.US, "  -> Tiempo de Lectura         : %s%n", MetricsTracker.formatDuration(res.readNanos()));
        System.out.printf(Locale.US, "  -> Tiempo Puro Selection Sort: %s%n", MetricsTracker.formatDuration(res.sortNanos()));
        System.out.printf(Locale.US, "  -> Tiempo de Escritura       : %s%n", MetricsTracker.formatDuration(res.writeNanos()));
        System.out.printf(Locale.US, "  -> Tiempo Total              : %s%n", MetricsTracker.formatTimeAndMem(totalNanos));

        if (res.sampleSize() < res.totalElementsFile()) {
            double hours = res.extrapolatedSeconds() / 3600.0;
            double days = hours / 24.0;
            System.out.println("  -----------------------------------------------------------------------------------------");
            System.out.printf(Locale.US, "  [EXTRAPOLACIÓN TEÓRICA O(n²) PARA N = %,d ELEMENTOS]%n", res.totalElementsFile());
            System.out.printf(Locale.US, "  -> T(N) ≈ T(M) * (N / M)² = %.2f segundos (~%.2f horas / ~%.2f días)%n",
                    res.extrapolatedSeconds(), hours, days);
            System.out.println("  -----------------------------------------------------------------------------------------");
        }

        metrics.record("e) Selection Sort (Muestra)", res.sampleSize(), totalNanos,
                String.format(Locale.US, "O(n²) | Extrap N: %.1f s", res.extrapolatedSeconds()));
    }

    /**
     * Opción f): Verificación del ordenamiento en streaming de los archivos de salida que existan en disco.
     */
    private void handleOptionF_VerifySortings() throws IOException {
        System.out.println("\n>>> [Opción f] Verificación en Streaming de Archivos de Ordenamiento (anterior <= actual)");
        String[] filesToCheck = {COUNTING_SORT_FILE, SELECTION_SORT_FILE, HEAP_SORT_FILE};
        boolean anyFound = false;

        System.out.println("  +----------------------+----------------+------------+--------------------------+");
        System.out.println("  | Archivo              | Elementos      | Estado     | Tiempo de Verificación   |");
        System.out.println("  +----------------------+----------------+------------+--------------------------+");

        for (String fileName : filesToCheck) {
            File f = new File(fileName);
            if (!f.exists()) {
                System.out.printf(Locale.US, "  | %-20s | %14s | %-10s | %-24s |%n",
                        fileName, "-", "NO EXISTE", "-");
                continue;
            }
            anyFound = true;
            FastIO.VerificationResult res = FastIO.verifySortedStreaming(f);
            String status = res.isSorted() ? "OK" : "ERROR";
            System.out.printf(Locale.US, "  | %-20s | %,14d | %-10s | %-24s |%n",
                    fileName, res.count(), status, MetricsTracker.formatDuration(res.elapsedNanos()));

            if (!res.isSorted()) {
                System.out.printf(Locale.US, "    [!] Ruptura de orden en índice %,d: anterior (%,d) > actual (%,d)%n",
                        res.errorIndex(), res.previousValue(), res.currentValue());
            }

            metrics.record("f) Verificación " + fileName, res.count(), res.elapsedNanos(), "Estado: " + status);
        }
        System.out.println("  +----------------------+----------------+------------+--------------------------+");
        if (!anyFound) {
            System.out.println("  [!] Ninguno de los archivos de salida existe aún. Ejecute las opciones d), e) o k) primero.");
        }
    }

    /**
     * Opción g): Tabla resumen de tiempos y métricas registradas.
     */
    private void handleOptionG_SummaryTable() {
        metrics.printSummaryTable();
    }

    /**
     * Opción h): Resumen de documentación técnica y decisiones de ingeniería en Java.
     */
    private void handleOptionH_DocumentationInfo() {
        System.out.println("\n>>> [Opción h] Documentación Técnica y Decisiones de Ingeniería en Java (JDK 17+)");
        System.out.println("  1. Cero Objetos Wrapper en Estructuras Masivas:");
        System.out.println("     Un Integer en Java consume 16-24 bytes frente a 4 bytes de un 'int' primitivo.");
        System.out.println("     Todo el TP opera exclusivamente con int[] y byte[] (~400 MB para 100.000.000 de int).");
        System.out.println("  2. Entrada/Salida (FastIO) por Bloques de 4 MB:");
        System.out.println("     Se reemplaza Scanner/BufferedReader por parseo y formateo ASCII directo sobre byte[4MB],");
        System.out.println("     evitando instanciar 100.000.000 de objetos String al leer o escribir Datos.txt (~900 MB).");
        System.out.println("  3. Árbol AVL con Arrays Paralelos (17 bytes/nodo):");
        System.out.println("     En lugar de objetos Node (~48 bytes/nodo + presión extrema al GC), AvlTree utiliza");
        System.out.println("     5 arrays paralelos (keys, left, right, height, count) y una Free List en O(1) para bajas.");
        System.out.println("  4. Informe Completo:");
        System.out.println("     Consulte los archivos 'README.md' e 'INFORME_TP3.md' en la raíz del proyecto.");
    }

    /**
     * Opción i): Submenú interactivo del Árbol AVL.
     */
    private void handleOptionI_AvlSubmenu() throws IOException {
        boolean inSubmenu = true;
        while (inSubmenu) {
            System.out.println("\n==================== SUBMENÚ i) ÁRBOL AVL (Arrays Paralelos) ====================");
            System.out.printf(Locale.US, "  Estado actual -> Nodos únicos: %,d | Total insertados: %,d | Altura: %d%n",
                    avlTree.getUniqueNodes(), avlTree.getTotalElements(), avlTree.getHeight());
            System.out.println("---------------------------------------------------------------------------------");
            System.out.println("  1. Construir árbol desde Datos.txt (midiendo tiempo y estimando RAM)");
            System.out.println("  2. Insertar un nuevo valor ingresado por el usuario");
            System.out.println("  3. Buscar un valor ingresado por el usuario (frecuencia y comparaciones)");
            System.out.println("  4. Eliminar un valor ingresado por el usuario (con rebalanceo AVL)");
            System.out.println("  5. Mostrar estadísticas y recorrido InOrden iterativo de los primeros N elementos");
            System.out.println("  0. Volver al menú principal");
            System.out.println("=================================================================================");

            String sub = readLinePrompt("Seleccione una opción del Árbol AVL (0-5): ").trim();
            switch (sub) {
                case "1" -> {
                    File file = requireDataFile();
                    if (file == null) break;

                    double maxMemMB = MetricsTracker.getMaxMemoryMB();
                    System.out.printf(Locale.US, "  Memoria máxima de la JVM (-Xmx): %.2f MB.%n", maxMemMB);
                    System.out.println("  Nota: Con 100M de enteros uniformes en [0, 100M], ~63.2% son claves únicas (~63.2M nodos),");
                    System.out.printf(Locale.US, "  lo que requiere ~%.0f MB de RAM con arrays paralelos (17 bytes/nodo).%n",
                            AvlTree.estimateMemoryMB(63_212_056));

                    // Recomendar un límite seguro según la RAM disponible en la JVM actual
                    int recommendedLimit = maxMemMB >= 2200 ? 0 : (int) Math.max(100_000, Math.min(5_000_000, (maxMemMB * 0.45 * 1024 * 1024) / AvlTree.BYTES_PER_NODE));
                    String prompt = recommendedLimit == 0
                            ? "  Ingrese límite M de elementos a cargar [Enter = 0 para cargar TODO el archivo]: "
                            : String.format(Locale.US, "  Ingrese límite M de elementos a cargar (0 = TODO el archivo) [Enter = %,d recomendados por RAM]: ", recommendedLimit);

                    int limit = readIntWithDefault(prompt, recommendedLimit, 0, Integer.MAX_VALUE);
                    int initialCap = limit > 0 ? Math.min(limit, 16_000_000) : 1_000_000;

                    avlTree = null;
                    System.gc();
                    avlTree = new AvlTree(initialCap);

                    long elapsed = avlTree.buildFromFile(file, limit);
                    System.out.println("  [OK] Árbol AVL construido exitosamente.");
                    System.out.printf(Locale.US, "  -> Elementos insertados : %,d%n", avlTree.getTotalElements());
                    System.out.printf(Locale.US, "  -> Nodos únicos         : %,d%n", avlTree.getUniqueNodes());
                    System.out.printf(Locale.US, "  -> Altura del árbol     : %d (Factor Balance Raíz = %d)%n",
                            avlTree.getHeight(), avlTree.isEmpty() ? 0 : avlTree.getBalanceFactor(0));
                    System.out.printf(Locale.US, "  -> Tiempo de ejecución  : %s%n", MetricsTracker.formatTimeAndMem(elapsed));

                    metrics.record("i.1) Construcción Árbol AVL", avlTree.getTotalElements(), elapsed,
                            String.format(Locale.US, "Únicos=%,d | Alt=%d", avlTree.getUniqueNodes(), avlTree.getHeight()));
                }
                case "2" -> {
                    int val = readRequiredInt("  Ingrese el valor entero a insertar en el AVL: ");
                    long start = System.nanoTime();
                    avlTree.insert(val);
                    long elapsed = System.nanoTime() - start;
                    AvlTree.SearchResult sr = avlTree.search(val);
                    System.out.printf(Locale.US, "  [OK] Valor %,d insertado. Frecuencia actual: %,d | Altura AVL: %d | Tiempo: %s%n",
                            val, sr.frequency(), avlTree.getHeight(), MetricsTracker.formatTimeAndMem(elapsed));
                    metrics.record("i.2) Inserción AVL", 1, elapsed, "Valor=" + val);
                }
                case "3" -> {
                    int val = readRequiredInt("  Ingrese el valor entero a buscar en el AVL: ");
                    AvlTree.SearchResult sr = avlTree.search(val);
                    if (sr.found()) {
                        System.out.printf(Locale.US, "  [ENCONTRADO] El valor %,d EXISTE en el Árbol AVL.%n", val);
                        System.out.printf(Locale.US, "  -> Frecuencia (apariciones): %,d%n", sr.frequency());
                    } else {
                        System.out.printf(Locale.US, "  [NO ENCONTRADO] El valor %,d NO existe en el Árbol AVL.%n", val);
                    }
                    System.out.printf(Locale.US, "  -> Comparaciones realizadas: %d (Complejidad O(log n))%n", sr.comparisons());
                    System.out.printf(Locale.US, "  -> Tiempo de búsqueda      : %s%n", MetricsTracker.formatTimeAndMem(sr.elapsedNanos()));
                    metrics.record("i.3) Búsqueda AVL", avlTree.getUniqueNodes(), sr.elapsedNanos(),
                            (sr.found() ? "Encontrado (f=" + sr.frequency() + ")" : "No encontrado"));
                }
                case "4" -> {
                    int val = readRequiredInt("  Ingrese el valor entero a eliminar del AVL: ");
                    long start = System.nanoTime();
                    boolean removed = avlTree.delete(val);
                    long elapsed = System.nanoTime() - start;
                    if (removed) {
                        System.out.printf(Locale.US, "  [OK] Valor %,d eliminado y árbol rebalanceado. Nodos únicos: %,d | Nueva altura: %d%n",
                                val, avlTree.getUniqueNodes(), avlTree.getHeight());
                    } else {
                        System.out.printf(Locale.US, "  [!] El valor %,d no se encontraba en el árbol.%n", val);
                    }
                    System.out.printf(Locale.US, "  -> Tiempo de eliminación   : %s%n", MetricsTracker.formatTimeAndMem(elapsed));
                    metrics.record("i.4) Eliminación AVL", avlTree.getUniqueNodes(), elapsed,
                            removed ? "Eliminado=" + val : "No existía=" + val);
                }
                case "5" -> {
                    int nShow = readIntWithDefault("  Cantidad N de elementos del recorrido InOrden a mostrar [Enter = 50]: ", 50, 1, 10_000);
                    long start = System.nanoTime();
                    int[] inOrder = avlTree.getInOrderFirstN(nShow);
                    long elapsed = System.nanoTime() - start;

                    System.out.println("\n  --- ESTADÍSTICAS DEL ÁRBOL AVL ---");
                    System.out.printf(Locale.US, "  -> Cantidad total de elementos insertados : %,d%n", avlTree.getTotalElements());
                    System.out.printf(Locale.US, "  -> Cantidad de nodos únicos               : %,d%n", avlTree.getUniqueNodes());
                    System.out.printf(Locale.US, "  -> Altura actual del árbol                : %d%n", avlTree.getHeight());
                    System.out.printf(Locale.US, "  -> Memoria estimada estructura AVL        : %.2f MB%n",
                            AvlTree.estimateMemoryMB(avlTree.getUniqueNodes()));
                    System.out.printf(Locale.US, "  -> Primeros %d elementos en InOrden (Ascendente):%n     ", inOrder.length);
                    for (int i = 0; i < inOrder.length; i++) {
                        System.out.printf(Locale.US, "%d%s", inOrder[i], (i < inOrder.length - 1) ? ", " : "");
                        if ((i + 1) % 10 == 0 && i < inOrder.length - 1) {
                            System.out.print("\n     ");
                        }
                    }
                    System.out.println();
                    System.out.printf(Locale.US, "  -> Tiempo de recorrido InOrden iterativo  : %s%n", MetricsTracker.formatDuration(elapsed));
                }
                case "0", "q" -> inSubmenu = false;
                default -> System.out.println("  [!] Opción inválida. Ingrese un número entre 0 y 5.");
            }
        }
    }

    /**
     * Opción j): Submenú interactivo del Heap Binario (Max-Heap y Min-Heap).
     */
    private void handleOptionJ_HeapSubmenu() throws IOException {
        System.out.println("\n>>> [Opción j] Submenú de Heap Binario (Montículo sobre int[])");
        String typeChoice = readLinePrompt("  ¿Desea trabajar con (1) Max-Heap o (2) Min-Heap? [Enter = 1 Max-Heap]: ").trim();
        boolean useMax = !typeChoice.equals("2");
        BinaryHeap activeHeap = useMax ? maxHeap : minHeap;
        String heapLabel = useMax ? "Max-Heap" : "Min-Heap";

        boolean inSubmenu = true;
        while (inSubmenu) {
            System.out.println("\n==================== SUBMENÚ j) " + heapLabel.toUpperCase(Locale.ROOT) + " (sobre int[]) ====================");
            System.out.printf(Locale.US, "  Estado actual -> Elementos: %,d | Raíz: %s | Altura aprox: %d%n",
                    activeHeap.getSize(),
                    activeHeap.isEmpty() ? "(vacío)" : String.format(Locale.US, "%,d", activeHeap.peekRoot()),
                    activeHeap.getApproximateHeight());
            System.out.println("---------------------------------------------------------------------------------");
            System.out.println("  1. Construir " + heapLabel + " desde Datos.txt (Algoritmo bottom-up de Floyd O(n))");
            System.out.println("  2. Insertar un nuevo elemento (siftUp O(log n))");
            System.out.println("  3. Buscar un valor (Búsqueda lineal O(n) con explicación teórica)");
            System.out.println("  4. Eliminar elemento raíz (" + (useMax ? "extraer Máximo" : "extraer Mínimo") + " en O(log n))");
            System.out.println("  5. Mostrar información del Heap (cantidad, raíz y altura floor(log2 n))");
            System.out.println("  6. Cambiar entre Max-Heap y Min-Heap");
            System.out.println("  0. Volver al menú principal");
            System.out.println("=================================================================================");

            String sub = readLinePrompt("Seleccione una opción para " + heapLabel + " (0-6): ").trim();
            switch (sub) {
                case "1" -> {
                    File file = requireDataFile();
                    if (file == null) break;

                    int limit = readIntWithDefault("  Ingrese límite de elementos a cargar [Enter = 0 para cargar TODO Datos.txt]: ", 0, 0, Integer.MAX_VALUE);
                    long elapsed = activeHeap.buildFromFile(file, limit);

                    System.out.println("  [OK] " + heapLabel + " construido mediante algoritmo bottom-up de Floyd en O(n).");
                    System.out.printf(Locale.US, "  -> Cantidad de elementos : %,d%n", activeHeap.getSize());
                    if (!activeHeap.isEmpty()) {
                        System.out.printf(Locale.US, "  -> Elemento Raíz (%s): %,d%n",
                                useMax ? "Máximo" : "Mínimo", activeHeap.peekRoot());
                    }
                    System.out.printf(Locale.US, "  -> Altura aproximada     : %d%n", activeHeap.getApproximateHeight());
                    System.out.printf(Locale.US, "  -> Tiempo de ejecución   : %s%n", MetricsTracker.formatTimeAndMem(elapsed));

                    metrics.record("j.1) Construcción " + heapLabel, activeHeap.getSize(), elapsed,
                            activeHeap.isEmpty() ? "Vacío" : "Raíz=" + activeHeap.peekRoot());
                }
                case "2" -> {
                    int val = readRequiredInt("  Ingrese el valor entero a insertar en el " + heapLabel + ": ");
                    long start = System.nanoTime();
                    activeHeap.insert(val);
                    long elapsed = System.nanoTime() - start;
                    System.out.printf(Locale.US, "  [OK] Valor %,d insertado. Nueva raíz: %,d | Total elementos: %,d | Tiempo: %s%n",
                            val, activeHeap.peekRoot(), activeHeap.getSize(), MetricsTracker.formatTimeAndMem(elapsed));
                    metrics.record("j.2) Inserción " + heapLabel, activeHeap.getSize(), elapsed, "Valor=" + val);
                }
                case "3" -> {
                    System.out.println("  [NOTA TEÓRICA]: Un Heap Binario solo impone un orden parcial vertical entre cada padre");
                    System.out.println("  y sus hijos (padre >= hijos en Max-Heap, padre <= hijos en Min-Heap), pero NO ordena");
                    System.out.println("  el subárbol izquierdo respecto al derecho. Por ello no admite búsqueda binaria y");
                    System.out.println("  cualquier búsqueda de un elemento arbitrario requiere recorrido lineal O(n).");

                    int val = readRequiredInt("  Ingrese el valor entero a buscar linealmente en el " + heapLabel + ": ");
                    BinaryHeap.SearchResult sr = activeHeap.searchLinear(val);
                    if (sr.found()) {
                        System.out.printf(Locale.US, "  [ENCONTRADO] El valor %,d se encuentra en el índice %,d del arreglo del Heap.%n",
                                val, sr.index());
                    } else {
                        System.out.printf(Locale.US, "  [NO ENCONTRADO] El valor %,d no está presente en el Heap.%n", val);
                    }
                    System.out.printf(Locale.US, "  -> Comparaciones realizadas: %,d%n", sr.comparisons());
                    System.out.printf(Locale.US, "  -> Tiempo de búsqueda      : %s%n", MetricsTracker.formatTimeAndMem(sr.elapsedNanos()));
                    metrics.record("j.3) Búsqueda Lineal " + heapLabel, activeHeap.getSize(), sr.elapsedNanos(),
                            sr.found() ? "Índice=" + sr.index() : "No encontrado");
                }
                case "4" -> {
                    if (activeHeap.isEmpty()) {
                        System.out.println("  [!] El " + heapLabel + " está vacío. Construya o inserte elementos primero.");
                        break;
                    }
                    long start = System.nanoTime();
                    int extracted = activeHeap.extractRoot();
                    long elapsed = System.nanoTime() - start;
                    System.out.printf(Locale.US, "  [OK] Raíz eliminada (%s extraído): %,d%n",
                            useMax ? "Máximo" : "Mínimo", extracted);
                    System.out.printf(Locale.US, "  -> Nueva raíz: %s | Elementos restantes: %,d%n",
                            activeHeap.isEmpty() ? "(vacío)" : String.format(Locale.US, "%,d", activeHeap.peekRoot()),
                            activeHeap.getSize());
                    System.out.printf(Locale.US, "  -> Tiempo de extracción: %s%n", MetricsTracker.formatTimeAndMem(elapsed));
                    metrics.record("j.4) Eliminar Raíz " + heapLabel, activeHeap.getSize(), elapsed, "Extraído=" + extracted);
                }
                case "5" -> {
                    System.out.println("\n  --- INFORMACIÓN DEL " + heapLabel.toUpperCase(Locale.ROOT) + " ---");
                    System.out.printf(Locale.US, "  -> Tipo de estructura        : %s (Transformar y Conquistar)%n", heapLabel);
                    System.out.printf(Locale.US, "  -> Cantidad de elementos (n) : %,d%n", activeHeap.getSize());
                    System.out.printf(Locale.US, "  -> Elemento en la raíz [0]   : %s%n",
                            activeHeap.isEmpty() ? "(vacío)" : String.format(Locale.US, "%,d", activeHeap.peekRoot()));
                    System.out.printf(Locale.US, "  -> Altura aproximada         : %d (floor(log2 n))%n", activeHeap.getApproximateHeight());
                    System.out.printf(Locale.US, "  -> Memoria del arreglo int[] : %.2f MB%n",
                            (activeHeap.getSize() * 4.0) / (1024.0 * 1024.0));
                }
                case "6" -> {
                    useMax = !useMax;
                    activeHeap = useMax ? maxHeap : minHeap;
                    heapLabel = useMax ? "Max-Heap" : "Min-Heap";
                    System.out.println("  [+] Cambiado a " + heapLabel + ".");
                }
                case "0", "q" -> inSubmenu = false;
                default -> System.out.println("  [!] Opción inválida. Ingrese un número entre 0 y 6.");
            }
        }
    }

    /**
     * Opción k): Ordenamiento mediante Heap Sort con verificación automática.
     */
    private void handleOptionK_HeapSort() throws IOException {
        File file = requireDataFile();
        if (file == null) return;

        System.out.println("\n>>> [Opción k] Ordenamiento mediante Heap Sort -> " + HEAP_SORT_FILE);
        System.out.println("  Cargando datos en int[], construyendo Max-Heap in-place O(n) y ordenando en O(n log n)...");

        File outFile = new File(HEAP_SORT_FILE);
        SortingAlgorithms.HeapSortResult res = SortingAlgorithms.heapSortFile(file, outFile);
        long totalNanos = res.readNanos() + res.buildHeapNanos() + res.sortNanos() + res.writeNanos();

        System.out.println("  [OK] Ordenamiento por Heap Sort finalizado.");
        System.out.printf(Locale.US, "  -> Elementos ordenados             : %,d%n", res.totalElements());
        System.out.printf(Locale.US, "  -> 1. Tiempo de Lectura (Datos.txt): %s%n", MetricsTracker.formatDuration(res.readNanos()));
        System.out.printf(Locale.US, "  -> 2. Construcción Max-Heap O(n)   : %s%n", MetricsTracker.formatDuration(res.buildHeapNanos()));
        System.out.printf(Locale.US, "  -> 3. Fase Heap Sort O(n log n)    : %s%n", MetricsTracker.formatDuration(res.sortNanos()));
        System.out.printf(Locale.US, "  -> 4. Escritura (s_HeapSort.txt)   : %s%n", MetricsTracker.formatDuration(res.writeNanos()));
        System.out.printf(Locale.US, "  -> 5. Verificación en Streaming    : %s [Estado: %s]%n",
                MetricsTracker.formatDuration(res.verification().elapsedNanos()),
                res.verification().isSorted() ? "OK - ORDENADO ASCENDENTEMENTE" : "ERROR");
        System.out.printf(Locale.US, "  -> Tiempo Total (1 a 4)            : %s%n", MetricsTracker.formatTimeAndMem(totalNanos));

        metrics.record("k) Heap Sort", res.totalElements(), totalNanos,
                "O(n log n) | Verif: " + (res.verification().isSorted() ? "OK" : "ERROR"));
    }

    private File requireDataFile() {
        File file = new File(DATA_FILE_NAME);
        if (!file.exists()) {
            System.out.println("  [!] El archivo '" + DATA_FILE_NAME + "' no existe. Por favor ejecute primero la opción a).");
            return null;
        }
        return file;
    }

    private boolean checkMemoryFor400MBArray(String description) {
        System.gc();
        double maxMB = MetricsTracker.getMaxMemoryMB();
        double usedMB = MetricsTracker.getUsedMemoryMB();
        double availableMB = maxMB - usedMB;
        if (availableMB < 420.0) {
            System.out.printf(Locale.US, "  [ADVERTENCIA] La operación requiere ~400 MB para '%s', y hay ~%.1f MB libres en la JVM.%n",
                    description, availableMB);
            System.out.println("  Se recomienda iniciar el programa con: java -Xmx6g -cp bin tp3.Main");
            String confirm = readLinePrompt("  ¿Desea intentar continuar de todos modos? (s/n) [s]: ").trim().toLowerCase(Locale.ROOT);
            return confirm.isEmpty() || confirm.startsWith("s") || confirm.startsWith("y");
        }
        return true;
    }

    private String readLinePrompt(String prompt) {
        System.out.print(prompt);
        try {
            String line = console.readLine();
            return line == null ? "q" : line;
        } catch (IOException e) {
            return "";
        }
    }

    private int readIntWithDefault(String prompt, int defaultVal, int min, int max) {
        while (true) {
            String input = readLinePrompt(prompt).trim().replace(".", "").replace("_", "");
            if (input.isEmpty()) {
                return defaultVal;
            }
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf(Locale.US, "  [!] Por favor ingrese un número entero entre %,d y %,d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Entrada inválida. Ingrese un número entero válido o presione Enter para usar el valor por defecto.");
            }
        }
    }

    private int readRequiredInt(String prompt) {
        while (true) {
            String input = readLinePrompt(prompt).trim().replace(".", "").replace("_", "");
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Entrada inválida. Por favor ingrese un número entero válido.");
            }
        }
    }
}

