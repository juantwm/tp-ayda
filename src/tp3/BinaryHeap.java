package tp3;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implementación de un <b>Montículo Binario (Binary Heap)</b> de alto rendimiento sobre un único
 * arreglo primitivo {@code int[]}, soportando tanto <b>Max-Heap</b> como <b>Min-Heap</b>.
 * <p>
 * Pertenece al paradigma <i>Transformar y Conquistar (Cambio de Representación)</i>.
 * Representa un árbol binario casi completo almacenado de forma implícita por niveles en un arreglo
 * contiguo sin punteros:
 * <ul>
 *   <li>{@code padre(i) = (i - 1) / 2}</li>
 *   <li>{@code hijoIzquierdo(i) = 2 * i + 1}</li>
 *   <li>{@code hijoDerecho(i) = 2 * i + 2}</li>
 * </ul>
 * <b>Complejidades:</b>
 * <ul>
 *   <li>Construcción bottom-up (Algoritmo de Floyd): Θ(n) tiempo, O(1) espacio auxiliar.</li>
 *   <li>Inserción ({@code siftUp}): O(log n).</li>
 *   <li>Eliminación de la raíz ({@code siftDown}): O(log n).</li>
 *   <li>Consulta del extremo (raíz): Θ(1).</li>
 *   <li>Búsqueda de un valor arbitrario: O(n) lineal (un Heap solo impone orden parcial padre-hijo,
 *       no orden total izquierda-derecha como un BST/AVL).</li>
 * </ul>
 * </p>
 *
 * @author Ingeniería de Software Senior - Análisis y Diseño de Algoritmos (UNViMe)
 */
public final class BinaryHeap {

    private int[] heap;
    private int size;
    private final boolean isMaxHeap;

    /**
     * Resultado de una búsqueda lineal dentro del Heap Binario.
     *
     * @param found        {@code true} si el elemento se encuentra en el Heap.
     * @param index        Primer índice del arreglo {@code int[]} donde se encontró (-1 si no existe).
     * @param comparisons  Cantidad de elementos inspeccionados.
     * @param elapsedNanos Tiempo empleado en nanosegundos.
     */
    public record SearchResult(boolean found, int index, int comparisons, long elapsedNanos) {}

    /**
     * Crea un Heap Binario vacío del tipo especificado.
     *
     * @param isMaxHeap       {@code true} para Max-Heap (raíz = máximo), {@code false} para Min-Heap (raíz = mínimo).
     * @param initialCapacity Capacidad inicial del arreglo subyacente.
     */
    public BinaryHeap(boolean isMaxHeap, int initialCapacity) {
        this.isMaxHeap = isMaxHeap;
        this.heap = new int[Math.max(16, initialCapacity)];
        this.size = 0;
    }

    public static int parent(int i) {
        return (i - 1) >>> 1;
    }

    public static int leftChild(int i) {
        return (i << 1) + 1;
    }

    public static int rightChild(int i) {
        return (i << 1) + 2;
    }

    /**
     * Construye el Heap en tiempo lineal Θ(n) a partir de los datos de un archivo aplicando el
     * algoritmo bottom-up de Floyd ({@code heapify}).
     *
     * @param file        Archivo de entrada ({@code Datos.txt}).
     * @param maxElements Límite máximo de elementos a cargar ({@code <= 0} para cargar todo el archivo).
     * @return Tiempo total empleado (lectura + construcción de Floyd) en nanosegundos.
     * @throws IOException Si ocurre un error de lectura.
     */
    public long buildFromFile(File file, int maxElements) throws IOException {
        long start = System.nanoTime();
        this.heap = FastIO.readIntsToArray(file, maxElements);
        this.size = this.heap.length;
        buildHeapFloyd();
        return System.nanoTime() - start;
    }

    /**
     * Construye el Heap directamente a partir de un arreglo existente en memoria en Θ(n).
     *
     * @param data Arreglo de enteros a copiar y transformar en Heap.
     */
    public void buildFromArray(int[] data) {
        this.heap = Arrays.copyOf(data, data.length);
        this.size = data.length;
        buildHeapFloyd();
    }

    /**
     * Algoritmo de Floyd para construir el Heap bottom-up en tiempo lineal Θ(n).
     * Todos los nodos con índice {@code >= size / 2} son hojas triviales que ya cumplen el invariante,
     * por lo que se ejecuta {@code siftDown} desde {@code (size / 2) - 1} hasta {@code 0}.
     */
    private void buildHeapFloyd() {
        for (int i = (size >>> 1) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    /**
     * Inserta un nuevo valor en el Heap en tiempo O(log n) ubicándolo al final del arreglo
     * y flotándolo mediante {@code siftUp}.
     *
     * @param value Nuevo entero a insertar.
     */
    public void insert(int value) {
        if (size == heap.length) {
            growCapacity();
        }
        heap[size] = value;
        siftUp(size);
        size++;
    }

    /**
     * Extrae y elimina el elemento raíz del Heap (el máximo en Max-Heap o el mínimo en Min-Heap)
     * en tiempo O(log n).
     *
     * @return El valor extremo que se encontraba en la raíz.
     * @throws IllegalStateException Si el Heap está vacío.
     */
    public int extractRoot() {
        if (size == 0) {
            throw new IllegalStateException("El Heap está vacío; no se puede eliminar la raíz.");
        }
        int rootValue = heap[0];
        size--;
        if (size > 0) {
            heap[0] = heap[size];
            siftDown(0);
        }
        return rootValue;
    }

    /**
     * Consulta el elemento raíz en tiempo Θ(1) sin eliminarlo.
     *
     * @return Valor en la raíz del Heap.
     * @throws IllegalStateException Si el Heap está vacío.
     */
    public int peekRoot() {
        if (size == 0) {
            throw new IllegalStateException("El Heap está vacío.");
        }
        return heap[0];
    }

    /**
     * Busca un valor en el Heap mediante recorrido lineal O(n).
     * <p>
     * <b>Justificación Teórica:</b> Un Heap Binario solo garantiza una relación de orden parcial vertical
     * entre cada nodo padre y sus hijos ({@code padre >= hijos} en Max-Heap o {@code padre <= hijos} en Min-Heap),
     * pero no impone ninguna relación de orden entre el subárbol izquierdo y el subárbol derecho. Por ello,
     * no es posible descartar mitades con búsqueda binaria y se requiere exploración lineal O(n).
     * </p>
     *
     * @param target Valor a buscar.
     * @return {@link SearchResult} con estado, índice encontrado, comparaciones y tiempo.
     */
    public SearchResult searchLinear(int target) {
        long start = System.nanoTime();
        // Cortocircuito O(1) si el valor buscado supera al máximo de un Max-Heap o es menor al mínimo de un Min-Heap
        if (size > 0) {
            if (isMaxHeap && target > heap[0]) {
                return new SearchResult(false, -1, 1, System.nanoTime() - start);
            }
            if (!isMaxHeap && target < heap[0]) {
                return new SearchResult(false, -1, 1, System.nanoTime() - start);
            }
        }

        for (int i = 0; i < size; i++) {
            if (heap[i] == target) {
                return new SearchResult(true, i, i + 1, System.nanoTime() - start);
            }
        }
        return new SearchResult(false, -1, size, System.nanoTime() - start);
    }

    /**
     * Operación {@code siftUp} (flotar): asciende un elemento recién insertado intercambiándolo
     * con su padre mientras viole la propiedad de Max-Heap o Min-Heap. Complejidad: O(log n).
     */
    private void siftUp(int idx) {
        int val = heap[idx];
        if (isMaxHeap) {
            while (idx > 0) {
                int p = (idx - 1) >>> 1;
                int parentVal = heap[p];
                if (val <= parentVal) {
                    break;
                }
                heap[idx] = parentVal;
                idx = p;
            }
        } else {
            while (idx > 0) {
                int p = (idx - 1) >>> 1;
                int parentVal = heap[p];
                if (val >= parentVal) {
                    break;
                }
                heap[idx] = parentVal;
                idx = p;
            }
        }
        heap[idx] = val;
    }

    /**
     * Operación {@code siftDown} (hundir): desciende un elemento intercambiándolo con el mayor
     * de sus hijos (en Max-Heap) o el menor de sus hijos (en Min-Heap). Complejidad: O(log n).
     */
    private void siftDown(int idx) {
        if (isMaxHeap) {
            siftDownMax(heap, idx, size);
        } else {
            siftDownMin(heap, idx, size);
        }
    }

    /**
     * Hundimiento estático optimizado para Max-Heap (reutilizado por {@link SortingAlgorithms#heapSortFile}).
     * Utiliza desplazamientos en lugar de intercambios triples en cada nivel (optimización de Floyd).
     *
     * @param arr      Arreglo del heap.
     * @param idx      Índice desde el cual hundir.
     * @param heapSize Tamaño activo del heap.
     */
    public static void siftDownMax(int[] arr, int idx, int heapSize) {
        int val = arr[idx];
        int half = heapSize >>> 1;
        while (idx < half) {
            int child = (idx << 1) + 1; // Hijo izquierdo
            int childVal = arr[child];
            int right = child + 1;
            if (right < heapSize && arr[right] > childVal) {
                child = right;
                childVal = arr[right];
            }
            if (val >= childVal) {
                break;
            }
            arr[idx] = childVal;
            idx = child;
        }
        arr[idx] = val;
    }

    /**
     * Hundimiento estático optimizado para Min-Heap.
     *
     * @param arr      Arreglo del heap.
     * @param idx      Índice desde el cual hundir.
     * @param heapSize Tamaño activo del heap.
     */
    public static void siftDownMin(int[] arr, int idx, int heapSize) {
        int val = arr[idx];
        int half = heapSize >>> 1;
        while (idx < half) {
            int child = (idx << 1) + 1;
            int childVal = arr[child];
            int right = child + 1;
            if (right < heapSize && arr[right] < childVal) {
                child = right;
                childVal = arr[right];
            }
            if (val <= childVal) {
                break;
            }
            arr[idx] = childVal;
            idx = child;
        }
        arr[idx] = val;
    }

    private void growCapacity() {
        int oldCap = heap.length;
        int newCap = oldCap + Math.max(16, oldCap >>> 1);
        if (newCap <= oldCap) {
            newCap = Integer.MAX_VALUE - 8;
        }
        heap = Arrays.copyOf(heap, newCap);
    }

    /**
     * Verifica que todos los nodos del Heap cumplan el invariante de orden parcial respecto a su padre.
     *
     * @return {@code true} si el Heap es válido.
     */
    public boolean validateHeapInvariant() {
        for (int i = 1; i < size; i++) {
            int p = (i - 1) >>> 1;
            if (isMaxHeap) {
                if (heap[p] < heap[i]) return false;
            } else {
                if (heap[p] > heap[i]) return false;
            }
        }
        return true;
    }

    /**
     * Calcula la altura aproximada del árbol binario casi completo: {@code floor(log2(size))}.
     *
     * @return Altura del Heap (-1 si está vacío, 0 si tiene 1 elemento).
     */
    public int getApproximateHeight() {
        if (size <= 0) return -1;
        return 31 - Integer.numberOfLeadingZeros(size);
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isMaxHeap() {
        return isMaxHeap;
    }
}

