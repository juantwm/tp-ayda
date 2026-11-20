# Trabajo Práctico N° 3: "Transformar y Conquistar"
**Materia:** Análisis y Diseño de Algoritmos (UNViMe)  
**Lenguaje:** Java 17+ Puro (Sin dependencias externas)

---

## 1. Arquitectura y Estructura del Proyecto

El proyecto está diseñado aplicando técnicas de **Java de Alto Rendimiento** para procesar hasta **100.000.000 de números enteros** en el rango `[0, 100.000.000]` (~900 MB en disco) sin saturar la memoria RAM ni el Garbage Collector (GC):

```text
TP3 - AYDA/
├── src/
│   └── tp3/
│       ├── Main.java               # Menú interactivo por consola (opciones a - k) y orquestación
│       ├── FastIO.java             # I/O bufferizado (4 MB) directo sobre byte[] sin instanciar String
│       ├── SortingAlgorithms.java  # Counting Sort O(n+k), Selection Sort O(n²) y Heap Sort O(n log n)
│       ├── AvlTree.java            # Árbol AVL auto-balanceado mediante 5 arrays primitivos paralelos
│       ├── BinaryHeap.java         # Max-Heap y Min-Heap sobre int[] (Algoritmo bottom-up de Floyd O(n))
│       └── MetricsTracker.java     # Medición de tiempos (System.nanoTime) y consumo de memoria JVM
├── README.md                       # Instrucciones de compilación y ejecución
└── INFORME_TP3.md                  # Informe académico comparativo listo para entregar
```

---

## 2. Requisitos Previos

* **JDK 17 o superior** (compatible con JDK 17, 21 LTS, etc.).
* Para verificar la versión instalada en consola:
  ```bash
  java -version
  javac -version
  ```

---

## 3. Instrucciones Exactas de Compilación

Ubicado en la carpeta raíz del proyecto (`TP3 - AYDA`), ejecute el siguiente comando para compilar todos los módulos dentro del directorio `bin`:

### En Windows (PowerShell o CMD):
```powershell
javac -encoding UTF-8 -Xlint:all -d bin src/tp3/*.java
```

### En Linux / macOS:
```bash
mkdir -p bin
javac -encoding UTF-8 -Xlint:all -d bin src/tp3/*.java
```

---

## 4. Instrucciones de Ejecución (Flags de Memoria JVM)

Para operar con el dataset completo de **100.000.000 de enteros**, es fundamental asignar suficiente memoria al Heap de la JVM (`-Xmx`):

### Ejecución Recomendada (Dataset completo de 100.000.000 de elementos):
```bash
java -Xms2g -Xmx6g -cp bin tp3.Main
```

### Ejecución en Equipos con 4 GB de RAM libre:
```bash
java -Xms1g -Xmx3500m -cp bin tp3.Main
```

> **Explicación de los Flags de la JVM:**
> * `-Xms2g`: Reserva inicialmente 2 GB de memoria Heap para evitar pausas de redimensionamiento durante la carga masiva.
> * `-Xmx6g`: Establece el límite máximo del Heap en 6 GB, permitiendo alojar cómodamente arreglos primitivos de 400 MB (`int[100_000_000]`) o los 5 arrays paralelos del Árbol AVL (~1.05 GB para ~63.2 millones de claves únicas).
> * `-cp bin`: Indica el classpath donde se encuentran los archivos `.class` compilados.

---

## 5. Guía Rápida de Uso del Menú Interactivo

1. **Opción `a)`**: Genera `Datos.txt`. Presione `Enter` para generar los `100.000.000` de enteros por defecto, o ingrese un número menor (ej. `50000` o `1000000`) para pruebas rápidas.
2. **Opción `b)`**: Recorre `Datos.txt` en streaming con memoria $O(1)$ e informa el mínimo, máximo y total de elementos.
3. **Opción `c)`**: Calcula en $O(n + k)$ la tabla con los **10 números que más se repiten** y su frecuencia exacta.
4. **Opción `d)`**: Ordena todo `Datos.txt` con **Counting Sort** en $O(n + k)$ y genera `s_CountingSort.txt`.
5. **Opción `e)`**: Ejecuta **Selection Sort** ($O(n^2)$) sobre una muestra configurable (por defecto `50.000` elementos), guarda `s_SelectionSort.txt` y calcula la extrapolación teórica $T(N) \approx T(M) \cdot (N/M)^2$ para los 100 millones.
6. **Opción `f)`**: Verifica en streaming que `s_CountingSort.txt`, `s_SelectionSort.txt` y `s_HeapSort.txt` estén ordenados ascendentemente (`anterior <= actual`).
7. **Opción `g)`**: Muestra en consola la tabla consolidada con todas las métricas de tiempo (`ms` y `s`) y memoria (`MB`) de la sesión.
8. **Opción `i)`**: Abre el submenú del **Árbol AVL** (construcción desde `Datos.txt`, inserción, búsqueda con frecuencia, eliminación con rebalanceo y recorrido InOrden iterativo).
9. **Opción `j)`**: Abre el submenú de **Heap Binario** permitiendo elegir entre **Max-Heap** y **Min-Heap** (construcción de Floyd $O(n)$, inserción, búsqueda lineal $O(n)$, eliminación de raíz y estadísticas).
10. **Opción `k)`**: Ordena `Datos.txt` con **Heap Sort** en $O(n \log n)$ in-place, genera `s_HeapSort.txt` y verifica automáticamente su ordenamiento.

