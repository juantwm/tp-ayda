package tp3;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implementación de un <b>Árbol Binario de Búsqueda Auto-balanceado AVL</b> (Adelson-Velsky y Landis)
 * perteneciente al paradigma <i>Transformar y Conquistar (Cambio de Representación)</i>.
 * <p>
 * <b>Optimización Crítica de Memoria en Java (Arquitectura de Arrays Paralelos):</b>
 * En una implementación orientada a objetos tradicional ({@code class Node}), cada instancia en una JVM
 * de 64 bits consume entre 40 y 48 bytes (cabecera del objeto + alineación + 3 referencias + campos {@code int}),
 * además de generar decenas de millones de referencias individuales que colapsan el Garbage Collector.
 * <br>
 * Esta clase reemplaza los objetos nodo por <b>5 arrays primitivos paralelos</b> indexados por un identificador
 * entero {@code nodeIdx}:
 * <ul>
 *   <li>{@code int[] keys}: Clave entera almacenada en el nodo.</li>
 *   <li>{@code int[] left}: Índice del hijo izquierdo ({@code -1} si es nulo).</li>
 *   <li>{@code int[] right}: Índice del hijo derecho ({@code -1} si es nulo).</li>
 *   <li>{@code byte[] height}: Altura del subárbol enraizado en el nodo (un AVL de 10^8 nodos tiene altura ≤ 39, cabe en 1 byte).</li>
 *   <li>{@code int[] count}: Frecuencia o multiplicidad de la clave para soportar duplicados en O(1) espacio extra.</li>
 * </ul>
 * De esta manera, cada nodo único ocupa exactamente <b>17 bytes</b> contiguos en memoria nativa de arreglos
 * y existen solo 5 objetos arreglo en el Heap de la JVM, eliminando por completo la sobrecarga del GC.
 * Incluye además una <b>Free List</b> intrusiva sobre el arreglo {@code left[]} para reciclar en O(1) los
 * índices de nodos eliminados.
 * </p>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class AvlTree {

    /** Constante que representa un puntero nulo en los arrays paralelos. */
    public static final int NULL_NODE = -1;

    /** Bytes consumidos por cada nodo único en la representación de 5 arrays paralelos (4+4+4+1+4 = 17 bytes). */
    public static final int BYTES_PER_NODE = 17;

    private int[] keys;
    private int[] left;
    private int[] right;
    private byte[] height;
    private int[] count;

    /** Índice de la raíz del árbol AVL (-1 si está vacío). */
    private int root = NULL_NODE;

    /** Cantidad de ranuras utilizadas (marca de agua alta en los arrays). */
    private int allocatedSlots = 0;

    /** Cabeza de la lista enlazada de índices reciclados tras eliminaciones (-1 si no hay libres). */
    private int freeListHead = NULL_NODE;

    /** Cantidad actual de claves únicas (nodos activos) en el árbol. */
    private int uniqueNodes = 0;

    /** Cantidad total de elementos insertados (incluyendo repeticiones/duplicados). */
    private long totalElements = 0;

    // Pilas preasignadas para operaciones iterativas y de actualización (altura máxima teórica AVL < 64)
    private final int[] pathNodes = new int[64];
    private final byte[] pathDirs = new byte[64]; // 0 = izquierda, 1 = derecha

    /**
     * Resultado de una búsqueda en el Árbol AVL.
     *
     * @param found        {@code true} si la clave existe en el árbol.
     * @param frequency    Cantidad de apariciones de la clave (0 si no existe).
     * @param comparisons  Cantidad de comparaciones de clave realizadas durante el descenso.
     * @param elapsedNanos Tiempo empleado en nanosegundos.
     */
    public record SearchResult(boolean found, int frequency, int comparisons, long elapsedNanos) {}

    /**
     * Crea un Árbol AVL con una capacidad inicial de nodos únicos.
     *
     * @param initialCapacity Capacidad inicial de los arrays paralelos.
     */
    public AvlTree(int initialCapacity) {
        int cap = Math.max(16, initialCapacity);
        this.keys = new int[cap];
        this.left = new int[cap];
        this.right = new int[cap];
        this.height = new byte[cap];
        this.count = new int[cap];
    }

    /**
     * Estima la memoria RAM requerida en Megabytes para almacenar {@code expectedUniqueNodes} nodos únicos.
     *
     * @param expectedUniqueNodes Cantidad estimada de claves únicas.
     * @return Memoria estimada en MB.
     */
    public static double estimateMemoryMB(int expectedUniqueNodes) {
        return (expectedUniqueNodes * (double) BYTES_PER_NODE) / (1024.0 * 1024.0);
    }

    /**
     * Reserva e inicializa un nuevo nodo en los arrays paralelos (reutilizando la Free List si hay nodos liberados).
     */
    private int allocateNode(int key) {
        int idx;
        if (freeListHead != NULL_NODE) {
            idx = freeListHead;
            freeListHead = left[idx]; // Avanza al siguiente nodo libre en la free list
        } else {
            if (allocatedSlots == keys.length) {
                growCapacity();
            }
            idx = allocatedSlots++;
        }
        keys[idx] = key;
        left[idx] = NULL_NODE;
        right[idx] = NULL_NODE;
        height[idx] = 1;
        count[idx] = 1;
        uniqueNodes++;
        return idx;
    }

    /**
     * Libera un nodo eliminándolo del árbol y agregando su índice a la Free List en O(1).
     */
    private void releaseNode(int idx) {
        left[idx] = freeListHead;
        right[idx] = NULL_NODE;
        height[idx] = 0;
        count[idx] = 0;
        freeListHead = idx;
        uniqueNodes--;
    }

    /**
     * Expande dinámicamente los 5 arrays paralelos en un 50% cuando se alcanza la capacidad actual.
     */
    private void growCapacity() {
        int oldCap = keys.length;
        int newCap = oldCap + (oldCap >>> 1);
        if (newCap <= oldCap) {
            newCap = Integer.MAX_VALUE - 8;
        }
        keys = Arrays.copyOf(keys, newCap);
        left = Arrays.copyOf(left, newCap);
        right = Arrays.copyOf(right, newCap);
        height = Arrays.copyOf(height, newCap);
        count = Arrays.copyOf(count, newCap);
    }

    /**
     * Obtiene la altura de un nodo (0 si el índice es {@code NULL_NODE}).
     */
    private int nodeHeight(int idx) {
        return idx == NULL_NODE ? 0 : height[idx];
    }

    /**
     * Recalcula y actualiza la altura de un nodo en función de las alturas de sus hijos.
     */
    private void updateHeight(int idx) {
        int hl = left[idx] == NULL_NODE ? 0 : height[left[idx]];
        int hr = right[idx] == NULL_NODE ? 0 : height[right[idx]];
        height[idx] = (byte) (1 + Math.max(hl, hr));
    }

    /**
     * Calcula el <b>Factor de Balance (FB)</b> de un nodo según la definición académica:
     * {@code FB(nodo) = altura(hijo_izquierdo) - altura(hijo_derecho)}.
     * En un árbol AVL válido, todo nodo cumple {@code FB ∈ {-1, 0, +1}}.
     *
     * @param idx Índice del nodo.
     * @return Factor de balance del nodo.
     */
    public int getBalanceFactor(int idx) {
        if (idx == NULL_NODE) return 0;
        int hl = left[idx] == NULL_NODE ? 0 : height[left[idx]];
        int hr = right[idx] == NULL_NODE ? 0 : height[right[idx]];
        return hl - hr;
    }

    /**
     * <b>Rotación Simple a la Derecha (Caso Izquierda-Izquierda / LL):</b>
     * Se aplica cuando un subárbol tiene {@code FB = +2} y su hijo izquierdo tiene {@code FB >= 0}.
     * <pre>
     *        y                  x
     *       / \                / \
     *      x   T3    ===>     T1  y
     *     / \                    / \
     *    T1  T2                 T2  T3
     * </pre>
     * Complejidad: O(1).
     */
    private int rotateRight(int y) {
        int x = left[y];
        int t2 = right[x];

        // Ejecutar rotación de enlaces
        right[x] = y;
        left[y] = t2;

        // Actualizar alturas (primero el nodo inferior y, luego la nueva raíz x)
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    /**
     * <b>Rotación Simple a la Izquierda (Caso Derecha-Derecha / RR):</b>
     * Se aplica cuando un subárbol tiene {@code FB = -2} y su hijo derecho tiene {@code FB <= 0}.
     * <pre>
     *      x                    y
     *     / \                  / \
     *    T1  y      ===>      x   T3
     *       / \              / \
     *      T2  T3           T1  T2
     * </pre>
     * Complejidad: O(1).
     */
    private int rotateLeft(int x) {
        int y = right[x];
        int t2 = left[y];

        // Ejecutar rotación de enlaces
        left[y] = x;
        right[x] = t2;

        // Actualizar alturas (primero el nodo inferior x, luego la nueva raíz y)
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    /**
     * <b>Rotación Doble Izquierda-Derecha (Caso LR):</b>
     * Se aplica cuando un subárbol tiene {@code FB = +2} y su hijo izquierdo tiene {@code FB < 0}.
     * Primero rota a la izquierda el hijo izquierdo y luego rota a la derecha el nodo desbalanceado.
     */
    private int rotateLeftRight(int node) {
        left[node] = rotateLeft(left[node]);
        return rotateRight(node);
    }

    /**
     * <b>Rotación Doble Derecha-Izquierda (Caso RL):</b>
     * Se aplica cuando un subárbol tiene {@code FB = -2} y su hijo derecho tiene {@code FB > 0}.
     * Primero rota a la derecha el hijo derecho y luego rota a la izquierda el nodo desbalanceado.
     */
    private int rotateRightLeft(int node) {
        right[node] = rotateRight(right[node]);
        return rotateLeft(node);
    }

    /**
     * Rebalancea un subárbol enraizado en {@code node} verificando su Factor de Balance y ejecutando
     * la rotación correspondiente (LL, RR, LR o RL) en tiempo O(1).
     */
    private int rebalanceNode(int node) {
        updateHeight(node);
        int fb = getBalanceFactor(node);

        // Desbalance hacia la izquierda (FB = +2)
        if (fb > 1) {
            if (getBalanceFactor(left[node]) >= 0) {
                return rotateRight(node); // Rotación Simple Derecha (LL)
            } else {
                return rotateLeftRight(node); // Rotación Doble Izquierda-Derecha (LR)
            }
        }

        // Desbalance hacia la derecha (FB = -2)
        if (fb < -1) {
            if (getBalanceFactor(right[node]) <= 0) {
                return rotateLeft(node); // Rotación Simple Izquierda (RR)
            } else {
                return rotateRightLeft(node); // Rotación Doble Derecha-Izquierda (RL)
            }
        }

        return node;
    }

    /**
     * Inserta una clave en el Árbol AVL de forma iterativa con rebalanceo bottom-up.
     * <p>
     * Si la clave ya existe en el árbol, incrementa su contador de frecuencia {@code count[idx]}
     * sin crear un nuevo nodo ni alterar la estructura topológica.
     * <br><b>Complejidad Temporal:</b> O(log n). <b>Complejidad Espacial Auxiliar:</b> O(1).
     * </p>
     *
     * @param key Valor entero a insertar.
     */
    public void insert(int key) {
        totalElements++;
        if (root == NULL_NODE) {
            root = allocateNode(key);
            return;
        }

        int curr = root;
        int depth = 0;

        while (curr != NULL_NODE) {
            int currKey = keys[curr];
            if (key == currKey) {
                count[curr]++;
                return; // Clave duplicada: solo se incrementa la frecuencia, las alturas no cambian
            }
            pathNodes[depth] = curr;
            if (key < currKey) {
                pathDirs[depth] = 0;
                curr = left[curr];
            } else {
                pathDirs[depth] = 1;
                curr = right[curr];
            }
            depth++;
        }

        int newNode = allocateNode(key);
        int parent = pathNodes[depth - 1];
        if (pathDirs[depth - 1] == 0) {
            left[parent] = newNode;
        } else {
            right[parent] = newNode;
        }

        // Ascenso por el camino recorrido actualizando alturas y rebalanceando
        for (int i = depth - 1; i >= 0; i--) {
            int node = pathNodes[i];
            int oldHeight = height[node];
            int balanced = rebalanceNode(node);

            if (i == 0) {
                root = balanced;
            } else {
                int ancestor = pathNodes[i - 1];
                if (pathDirs[i - 1] == 0) {
                    left[ancestor] = balanced;
                } else {
                    right[ancestor] = balanced;
                }
            }

            // En inserción, si la raíz del subárbol conserva la misma altura tras rebalancear,
            // ningún ancestro superior pudo haberse desbalanceado (corte temprano O(1)).
            if (balanced == node && height[node] == oldHeight) {
                break;
            }
        }
    }

    /**
     * Construye el Árbol AVL leyendo hasta {@code maxElements} enteros desde {@code file} en streaming.
     *
     * @param file        Archivo de origen ({@code Datos.txt}).
     * @param maxElements Límite de elementos a cargar (o {@code <= 0} para cargar el archivo completo).
     * @return Tiempo empleado en nanosegundos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public long buildFromFile(File file, int maxElements) throws IOException {
        long start = System.nanoTime();
        int readCount = 0;
        int progressStep = maxElements > 0 ? Math.max(1_000_000, maxElements / 10) : 10_000_000;

        try (FastIO.FastIntReader reader = new FastIO.FastIntReader(file)) {
            int val;
            while ((maxElements <= 0 || readCount < maxElements) && (val = reader.nextIntOrEof()) != Integer.MIN_VALUE) {
                insert(val);
                readCount++;
                if (readCount % progressStep == 0) {
                    System.out.printf("  -> AVL cargados: %,d elementos | Nodos únicos: %,d | Altura actual: %d%n",
                            readCount, uniqueNodes, getHeight());
                }
            }
        }
        return System.nanoTime() - start;
    }

    /**
     * Busca una clave en el Árbol AVL en tiempo O(log n).
     *
     * @param key Clave entera a buscar.
     * @return {@link SearchResult} indicando si existe, su frecuencia, comparaciones y tiempo.
     */
    public SearchResult search(int key) {
        long start = System.nanoTime();
        int curr = root;
        int comparisons = 0;

        while (curr != NULL_NODE) {
            comparisons++;
            int currKey = keys[curr];
            if (key == currKey) {
                return new SearchResult(true, count[curr], comparisons, System.nanoTime() - start);
            } else if (key < currKey) {
                curr = left[curr];
            } else {
                curr = right[curr];
            }
        }
        return new SearchResult(false, 0, comparisons, System.nanoTime() - start);
    }

    /**
     * Elimina una clave completamente del Árbol AVL (incluyendo todas sus apariciones)
     * y restaura el balance AVL mediante rotaciones en tiempo O(log n).
     *
     * @param key Clave a eliminar.
     * @return {@code true} si la clave existía y fue eliminada; {@code false} si no se encontró.
     */
    public boolean delete(int key) {
        int existingFreq = getFrequency(key);
        if (existingFreq == 0) {
            return false;
        }
        root = deleteRecursive(root, key);
        totalElements -= existingFreq;
        return true;
    }

    private int getFrequency(int key) {
        int curr = root;
        while (curr != NULL_NODE) {
            int currKey = keys[curr];
            if (key == currKey) return count[curr];
            curr = (key < currKey) ? left[curr] : right[curr];
        }
        return 0;
    }

    /**
     * Eliminación recursiva sobre índices de arrays paralelos (la profundidad máxima es ≤ 45,
     * por lo que la pila de llamadas consume menos de 2 KB y jamás produce StackOverflow).
     */
    private int deleteRecursive(int node, int key) {
        if (node == NULL_NODE) {
            return NULL_NODE;
        }

        int nodeKey = keys[node];
        if (key < nodeKey) {
            left[node] = deleteRecursive(left[node], key);
        } else if (key > nodeKey) {
            right[node] = deleteRecursive(right[node], key);
        } else {
            // Nodo encontrado: verificar cantidad de hijos
            if (left[node] == NULL_NODE || right[node] == NULL_NODE) {
                int child = (left[node] != NULL_NODE) ? left[node] : right[node];
                releaseNode(node);
                return child;
            } else {
                // Caso con 2 hijos: reemplazar por el sucesor InOrden (mínimo del subárbol derecho)
                int successor = findMinNode(right[node]);
                keys[node] = keys[successor];
                count[node] = count[successor];
                // Eliminamos el nodo sucesor físico (evitando restar dos veces uniqueNodes ajustando temporalmente)
                right[node] = deleteMinNode(right[node]);
            }
        }

        return rebalanceNode(node);
    }

    private int findMinNode(int node) {
        int curr = node;
        while (left[curr] != NULL_NODE) {
            curr = left[curr];
        }
        return curr;
    }

    /**
     * Desvincula y libera el nodo con la clave mínima de un subárbol, rebalanceando en el ascenso.
     */
    private int deleteMinNode(int node) {
        if (left[node] == NULL_NODE) {
            int rightChild = right[node];
            releaseNode(node);
            return rightChild;
        }
        left[node] = deleteMinNode(left[node]);
        return rebalanceNode(node);
    }

    /**
     * Recorrido <b>InOrden Iterativo</b> (Izquierda -> Raíz -> Derecha) que recupera los primeros
     * {@code limit} valores únicos ordenados ascendentemente sin usar recursión (evitando cualquier
     * riesgo de {@link StackOverflowError}).
     *
     * @param limit Cantidad máxima de claves ordenadas a devolver.
     * @return Arreglo con las primeras {@code limit} claves ordenadas de menor a mayor.
     */
    public int[] getInOrderFirstN(int limit) {
        int target = Math.min(Math.max(0, limit), uniqueNodes);
        int[] result = new int[target];
        if (target == 0 || root == NULL_NODE) {
            return result;
        }

        int[] stack = new int[Math.max(64, getHeight() + 4)];
        int top = 0;
        int curr = root;
        int idx = 0;

        while ((curr != NULL_NODE || top > 0) && idx < target) {
            while (curr != NULL_NODE) {
                stack[top++] = curr;
                curr = left[curr];
            }
            curr = stack[--top];
            result[idx++] = keys[curr];
            curr = right[curr];
        }
        return result;
    }

    /**
     * Verifica recursivamente que todo el Árbol AVL cumpla de manera estricta:
     * <ol>
     *   <li>Propiedad de Árbol Binario de Búsqueda (BST): {@code izq < nodo < der}.</li>
     *   <li>Invariante de Balance AVL en cada nodo: {@code FB ∈ {-1, 0, +1}}.</li>
     *   <li>Consistencia exacta del atributo {@code height} almacenado.</li>
     * </ol>
     *
     * @return {@code true} si el árbol cumple todos los invariantes matemáticos de un AVL.
     */
    public boolean validateAvlInvariants() {
        return validateSubtree(root, Integer.MIN_VALUE, Integer.MAX_VALUE) >= 0;
    }

    private int validateSubtree(int node, long minAllowed, long maxAllowed) {
        if (node == NULL_NODE) {
            return 0;
        }
        int k = keys[node];
        if (k < minAllowed || k > maxAllowed) {
            return -1; // Violación de propiedad BST
        }
        int hl = validateSubtree(left[node], minAllowed, (long) k - 1);
        if (hl < 0) return -1;
        int hr = validateSubtree(right[node], (long) k + 1, maxAllowed);
        if (hr < 0) return -1;

        int fb = hl - hr;
        if (fb < -1 || fb > 1) {
            return -1; // Violación del factor de balance AVL
        }
        int expectedHeight = 1 + Math.max(hl, hr);
        if (height[node] != expectedHeight) {
            return -1; // Altura desincronizada
        }
        return expectedHeight;
    }

    public int getHeight() {
        return nodeHeight(root);
    }

    public int getUniqueNodes() {
        return uniqueNodes;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getRootKey() {
        return root == NULL_NODE ? 0 : keys[root];
    }

    public boolean isEmpty() {
        return root == NULL_NODE;
    }
}

