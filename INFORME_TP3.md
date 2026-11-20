# UNIVERSIDAD NACIONAL DE VILLA MERCEDES (UNViMe)
## Escuela de Ingeniería y Ciencias Ambientales
### Cátedra: Análisis y Diseño de Algoritmos

---

# TRABAJO PRÁCTICO N° 3: "Transformar y Conquistar"
### Procesamiento Masivo de Datos ($10^8$ Enteros), Árboles AVL, Montículos Binarios y Algoritmos de Ordenamiento en Java

---

**Integrantes / Alumno:**
* **Apellido y Nombre:** `[COMPLETAR APELLIDO Y NOMBRE]`
* **DNI:** `[COMPLETAR DNI]`
* **Carrera:** Ingeniería en Sistemas de Información / Programador Universitario de Sistemas
* **Docentes:** `[COMPLETAR EQUIPO DOCENTE]`
* **Fecha de Entrega:** `[COMPLETAR FECHA]`

---

## 1. Introducción y Objetivos

El presente informe aborda el desarrollo e implementación en **Java (JDK 17+)** del **Trabajo Práctico N° 3 ("Transformar y Conquistar")**, cuyo objetivo central es analizar teórica y empíricamente el comportamiento de algoritmos de búsqueda, conteo de frecuencias, ordenamiento y estructuras de datos avanzadas frente a un volumen masivo de **$N = 100.000.000$ de números enteros** distribuidos uniformemente en el intervalo $[0, 100.000.000]$.

Operar con $10^8$ registros impone restricciones físicas severas tanto en tiempo de CPU como en jerarquía de memoria y almacenamiento (~900 MB en texto plano). A lo largo del trabajo se contrasta el paradigma de **Fuerza Bruta** (Selection Sort y búsqueda lineal) frente al paradigma de **Transformar y Conquistar** en sus variantes de:
1. **Simplificación de la instancia / Preordenamiento:** Ordenamiento previo y verificación lineal en $O(n)$.
2. **Cambio de Representación:**
   * **Árbol AVL:** Transformación del conjunto de datos en un árbol binario de búsqueda estrictamente balanceado ($FB \in \{-1, 0, 1\}$) para garantizar operaciones dinámicas en $O(\log n)$.
   * **Heap Binario (Max-Heap y Min-Heap) y Heap Sort:** Transformación de un arreglo desordenado en un árbol binario casi completo implícito con orden parcial en $\Theta(n)$ mediante el algoritmo de Floyd, permitiendo extracción del extremo en $O(\log n)$ y ordenamiento in-place en $O(n \log n)$ con espacio auxiliar $O(1)$.
   * **Counting Sort:** Transformación del dominio de valores a un vector de direccionamiento directo por conteo de frecuencias en $\Theta(n + k)$.

---

## 2. Decisiones de Diseño e Ingeniería en Java para Soportar 100 Millones de Registros

En la Máquina Virtual de Java (JVM), el manejo ingenuo de $100.000.000$ de datos mediante colecciones genéricas (`ArrayList<Integer>`, `HashMap<Integer, Integer>`) o nodos basados en objetos (`class Node`) provoca un colapso inmediato por `OutOfMemoryError` y pausas críticas del Garbage Collector (GC). Para resolver este desafío se implementaron cinco decisiones arquitectónicas clave:

### 2.1. Uso Exclusivo de Arrays Primitivos (`int[]`, `byte[]`) vs. Objetos Wrapper (`Integer`)
* En una JVM de 64 bits, cada instancia de `java.lang.Integer` consume **16 a 24 bytes** (12 bytes de cabecera de objeto + 4 bytes del valor + alineación), más **4 a 8 bytes** por la referencia en una colección. Almacenar $10^8$ objetos `Integer` en un `ArrayList<Integer>` consumiría entre **2.0 GB y 2.8 GB** y crearía 100 millones de referencias individuales que el GC debe rastrear.
* En contraste, un arreglo primitivo `int[100_000_000]` es un **único objeto** en el Heap de la JVM con memoria contigua de $100.000.000 \times 4\text{ bytes} = 400.000.000\text{ bytes} \approx \mathbf{381.47\text{ MB}}$, maximizando la localidad espacial en la caché L1/L2/L3 del procesador y reduciendo a cero la sobrecarga del GC.

### 2.2. Entrada/Salida (I/O) de Ultra Alto Rendimiento (`FastIO`)
* El archivo `Datos.txt` con $10^8$ números en representación ASCII ocupa aproximadamente **890 MB**.
* Utilizar `java.util.Scanner` o `BufferedReader.readLine()` + `Integer.parseInt()` obligaría a instanciar $100.000.000$ de objetos `String` y `Matcher` temporales (~4 GB de basura efímera en el Young Generation del Heap).
* Se diseñó la clase `FastIO` con lectores (`FastIntReader`) y escritores (`FastIntWriter`) que operan sobre un **buffer fijo de 4 MB (`byte[4 * 1024 * 1024]`)**. Los dígitos ASCII se acumulan directamente mediante aritmética entera (`val = val * 10 + (b - '0')`) en lectura y división por 10 en escritura, logrando **asignación cero de objetos (`zero-allocation`)** en el bucle crítico.
* Para la generación aleatoria se empleó `SplittableRandom` en lugar de `java.util.Random`, evitando bloqueos atómicos (`AtomicLong CAS`) y triplicando el caudal de generación.

### 2.3. Arquitectura de Arrays Paralelos en el Árbol AVL (`AvlTree`)
* Un nodo tradicional `class AvlNode { int key, count, height; AvlNode left, right; }` ocupa **40 a 48 bytes** en la JVM. Para los $\approx 63.212.000$ valores únicos esperados al generar $10^8$ enteros uniformes en $[0, 10^8]$ (dado que $1 - e^{-1} \approx 63.21\%$), se requerirían más de **3 GB de RAM** solo en nodos y el GC colapsaría al recorrer 126 millones de punteros.
* Se reemplazaron los objetos nodo por **5 arrays primitivos paralelos** indexados por un entero `idx` (`-1` representa `null`):
  * `int[] keys` (4 bytes): Clave del nodo.
  * `int[] left` (4 bytes): Índice del hijo izquierdo.
  * `int[] right` (4 bytes): Índice del hijo derecho.
  * `byte[] height` (1 byte): Altura del subárbol (la altura máxima de un AVL con $10^8$ nodos es $\le 39$, por lo que cabe holgadamente en un `byte`).
  * `int[] count` (4 bytes): Frecuencia de la clave para absorber duplicados sin crear nodos repetidos.
* **Resultado:** Cada nodo único consume exactamente **17 bytes** contiguos (~1.02 GB para 63.2M de nodos únicos) y existen únicamente 5 objetos arreglo en toda la JVM. Además, al eliminar nodos, sus índices se reciclan en $O(1)$ mediante una **Free List** intrusiva sobre el propio arreglo `left[]`.

### 2.4. Optimización de Memoria en Counting Sort
* En el Counting Sort estable clásico sobre registros complejos se requieren tres arreglos (`input[n]`, `count[k+1]` y `output[n]`), lo que insumiría $3 \times 400\text{ MB} = 1.2\text{ GB}$.
* Tratándose de enteros puros sin datos satélites distinguibles, `SortingAlgorithms.countingSortToFile` acumula las frecuencias leyendo `Datos.txt` en streaming sobre `int[] freq = new int[100_000_001]` (~381.5 MB) y emite directamente los valores ordenados al buffer de escritura, reduciendo el uso de RAM en un **66%**. Asimismo, se dejó implementada y documentada en código la variante clásica con sumas acumuladas (`classicStableCountingSortInMemory`).

### 2.5. Manejo Inteligente del Cuello de Botella $O(n^2)$ en Selection Sort
* Ordenar $N = 100.000.000$ de elementos con Selection Sort exige $\frac{N(N-1)}{2} \approx 5 \times 10^{15}$ comparaciones. Incluso a $10^9$ comparaciones por segundo, demoraría $\approx 5 \times 10^6\text{ segundos} \approx \mathbf{57.8\text{ días}}$.
* El sistema detecta automáticamente cuando $N > 100.000$, advierte al usuario, ejecuta la prueba real sobre una muestra configurable $M$ (por defecto $M = 50.000$), guarda `s_SelectionSort.txt` y calcula la extrapolación cuadrática exacta:
  $$T(N) \approx T(M) \cdot \left(\frac{N}{M}\right)^2$$

---

## 3. Análisis Comparativo de Estructuras de Datos: Árbol AVL vs. Heap Binario

Ambas estructuras pertenecen al paradigma **Transformar y Conquistar (Cambio de Representación)**, pero responden a invariantes matemáticos y casos de uso radicalmente distintos:

| Criterio / Operación | Árbol AVL (Arrays Paralelos) | Heap Binario (Max-Heap / Min-Heap sobre `int[]`) |
| :--- | :--- | :--- |
| **Invariante de Orden** | **Orden Total (BST):** Para todo nodo $x$, $\text{claves}(izq) < x < \text{claves}(der)$. | **Orden Parcial (Dominancia Parental):** $\text{padre} \ge \text{hijos}$ (Max-Heap) o $\text{padre} \le \text{hijos}$ (Min-Heap). |
| **Invariante Estructural** | Factor de Balance $FB = h(izq) - h(der) \in \{-1, 0, +1\}$ en todo nodo. Altura $h \le 1.44 \log_2(n)$. | Árbol binario **casi completo** lleno de izquierda a derecha. Altura exacta $h = \lfloor \log_2 n \rfloor$. |
| **Construcción Masiva ($n$ datos)** | $O(n \log n)$ insertando elemento a elemento con rotaciones. | **$\Theta(n)$** mediante el algoritmo bottom-up de Floyd (`heapify`). |
| **Inserción de un elemento** | **$O(\log n)$** (descenso BST + ascenso con hasta 1 rotación simple o doble). | **$O(\log n)$** en peor caso y **$O(1)$** en promedio (`siftUp`). |
| **Búsqueda de clave arbitraria** | **$O(\log n)$** en el peor caso gracias al orden total y balanceo estricto. | **$O(n)$ (Búsqueda Lineal):** No existe orden entre subárbol izquierdo y derecho, impidiendo búsqueda binaria. |
| **Eliminación** | **$O(\log n)$** para eliminar cualquier clave arbitraria (con hasta $O(\log n)$ rotaciones). | **$O(\log n)$** para eliminar la **raíz** (`siftDown`). |
| **Acceso al Extremo (Máx / Mín)** | $O(\log n)$ descendiendo al extremo derecho o izquierdo (o $O(1)$ si se cachea). | **$\Theta(1)$** inmediato en la raíz `heap[0]`. |
| **Recorrido Ordenado Completo** | **$\Theta(n)$** natural mediante recorrido **InOrden** iterativo sin modificar el árbol. | **$O(n \log n)$** extrayendo sucesivamente la raíz (destruye el heap o lo ordena vía Heap Sort). |
| **Uso de Memoria en Java** | **17 bytes por nodo único** (5 arrays paralelos: `keys`, `left`, `right`, `height`, `count`). | **4 bytes por elemento** (un único arreglo `int[n]` sin punteros; representación implícita). |

---

## 4. Análisis Comparativo de Algoritmos de Ordenamiento

| Característica | Counting Sort | Selection Sort | Heap Sort |
| :--- | :--- | :--- | :--- |
| **Paradigma Algorítmico** | Transformar y Conquistar (Distribución / Conteo por direccionamiento directo) | **Fuerza Bruta** (Selección exhaustiva del mínimo restante) | **Transformar y Conquistar** (Cambio de representación a Max-Heap in-place) |
| **Tipo de Algoritmo** | **No comparativo** (basado en operaciones aritméticas de índice) | Comparativo | Comparativo |
| **Complejidad Mejor Caso** | $\Theta(n + k)$ | $\Theta(n^2)$ | $\Theta(n \log n)$ (o $O(n)$ si todos los elementos son idénticos) |
| **Complejidad Caso Promedio** | $\Theta(n + k)$ | $\Theta(n^2)$ | $\Theta(n \log n)$ |
| **Complejidad Peor Caso** | $\Theta(n + k)$ | $\Theta(n^2)$ | $\Theta(n \log n)$ |
| **Espacio Auxiliar en RAM** | $\Theta(k)$ ($\approx 381.5\text{ MB}$ para `int[100_000_001]`) | **$O(1)$** (estrictamente in-place sobre el arreglo de muestra) | **$O(1)$** (estrictamente in-place sobre el `int[n]` cargado) |
| **Estabilidad** | **Sí** (en su formulación con sumas acumuladas; irrelevante en enteros atómicos puros) | **No** (los intercambios de largo alcance alteran el orden relativo de claves iguales) | **No** (las extracciones de la raíz al final del arreglo rompen la estabilidad) |
| **Viabilidad para $N = 10^8$** | **Óptima** (el más veloz cuando $k = O(n)$ cabe en memoria principal) | **Inviable** ($\approx 5 \times 10^{15}$ comparaciones; requiere muestreo y extrapolación) | **Muy Alta** (garantiza cota $O(n \log n)$ sin riesgo de peor caso cuadrático ni memoria extra) |

---

## 5. Resultados Empíricos y Evidencia de Ejecución

> **Nota para la entrega:** Complete los valores `[COMPLETAR CON TIEMPO REAL]` con las mediciones arrojadas por la opción `g)` de su consola al ejecutar `java -Xms2g -Xmx6g -cp bin tp3.Main`, e inserte las capturas de pantalla correspondientes en los bloques `[INSERTAR CAPTURA]`.

### 5.1. Tabla General de Tiempos Medidos ($N = 100.000.000$)

| Opción | Operación / Algoritmo | Tamaño Procesado ($N$) | Tiempo Medido (ms) | Tiempo Medido (s) | Memoria JVM Usada (MB) | Resultado / Observaciones |
| :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| **a)** | Generación de `Datos.txt` | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Archivo de $\approx 888.89\text{ MB}$ |
| **b)** | Búsqueda de Máximo y Mínimo | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Mín: `0`, Máx: `100.000.000`, RAM $O(1)$ |
| **c)** | Análisis de Frecuencia (Top 10) | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Complejidad $O(n + k)$ |
| **d)** | Ordenamiento Counting Sort | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Salida en `s_CountingSort.txt` |
| **e)** | Selection Sort (Muestra $M = 50.000$) | $50.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Extrapolación $N=10^8$: `[COMPLETAR]` días |
| **f)** | Verificación de Ordenamiento | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Estado: `OK` en todos los archivos |
| **i.1)** | Construcción Árbol AVL | `[COMPLETAR N]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Nodos únicos: `[COMPLETAR]`, Altura: `[COMPLETAR]` |
| **i.2)** | Inserción en Árbol AVL | $1$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Complejidad $O(\log n)$ |
| **i.3)** | Búsqueda en Árbol AVL | $1$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Comparaciones $\le 40$ |
| **i.4)** | Eliminación en Árbol AVL | $1$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Con rebalanceo automático |
| **j.1)** | Construcción Heap Binario (Floyd) | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Construcción in-place $\Theta(n)$ |
| **j.2)** | Inserción en Heap Binario | $1$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Operación `siftUp` $O(\log n)$ |
| **j.3)** | Búsqueda Lineal en Heap Binario | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Recorrido lineal $O(n)$ |
| **j.4)** | Eliminación de Raíz en Heap | $1$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Operación `siftDown` $O(\log n)$ |
| **k)** | Ordenamiento Heap Sort | $100.000.000$ | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR CON TIEMPO REAL]` | `[COMPLETAR]` | Salida verificada en `s_HeapSort.txt` |

---

### 5.2. Capturas de Pantalla por Opción del Menú

* **Opción a) Generación de `Datos.txt`:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN A]`

* **Opción b) Búsqueda del Máximo y Mínimo:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN B]`

* **Opción c) Análisis de Frecuencia (Top 10):**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN C]`

* **Opción d) Ordenamiento mediante Counting Sort:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN D]`

* **Opción e) Ordenamiento mediante Selection Sort y Extrapolación:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN E]`

* **Opción f) Verificación del Ordenamiento en Streaming:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN F]`

* **Opción g) Tabla Resumen Consolidada de la Sesión:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN G]`

* **Opción i) Submenú del Árbol AVL (Construcción, Inserción, Búsqueda, Eliminación e InOrden):**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN I]`

* **Opción j) Submenú del Heap Binario (Max-Heap y Min-Heap):**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN J]`

* **Opción k) Ordenamiento mediante Heap Sort y Verificación Automática:**  
  `[INSERTAR CAPTURA DE CONSOLA - OPCIÓN K]`

---

## 6. Conclusiones Técnicas Fundamentadas

1. **Superioridad Asintótica del Paradigma "Transformar y Conquistar" frente a "Fuerza Bruta" en $N = 10^8$:**  
   Los resultados empíricos demuestran que cuando el volumen de datos escala a $100.000.000$ de registros, la complejidad cuadrática $\Theta(n^2)$ de **Selection Sort** deja de ser una alternativa lenta para convertirse en una barrera computacional infranqueable ($\approx 5 \times 10^{15}$ comparaciones, equivalentes a semanas de ejecución). En contrapartida, transformar la representación del arreglo en un **Max-Heap** permite a **Heap Sort** ordenar exactamente el mismo conjunto en $\Theta(n \log n)$ ($\approx 2.65 \times 10^9$ operaciones), reduciendo el tiempo de semanas a pocos segundos con espacio auxiliar $O(1)$.

2. **Aprovechamiento del Dominio Acotado con Counting Sort ($\Theta(n + k)$):**  
   Al conocer a priori que las claves son enteros dentro del rango $[0, 100.000.000]$ ($k = n = 10^8$), **Counting Sort** rompe la cota inferior teórica $\Omega(n \log n)$ de los algoritmos basados en comparaciones. Su complejidad lineal $\Theta(n + k)$ y su patrón de acceso secuencial al escribir los resultados lo convierten en el algoritmo más rápido del estudio, a cambio de reservar un vector auxiliar de conteo de $\approx 381.5\text{ MB}$.

3. **Compromiso Estructural entre Árbol AVL y Heap Binario:**  
   * El **Árbol AVL** impone un **orden total** balanceado ($FB \in \{-1, 0, +1\}$). Es la estructura indispensable cuando el problema exige **búsquedas exactas de cualquier clave en $O(\log n)$**, contabilización dinámica de duplicados, eliminaciones arbitrarias en $O(\log n)$ y recorridos ordenados (**InOrden**) instantáneos. Sin embargo, ese orden total tiene un costo mayor en memoria (17 bytes por nodo único en nuestra implementación optimizada de arrays paralelos) y una constante de tiempo más alta debido a los saltos de memoria no contiguos (cache misses) y rotaciones.
   * El **Heap Binario** relaja el requisito a un **orden parcial** almacenado implícitamente en un único arreglo contiguo (`4 bytes` por elemento, sin punteros). Gracias a ello, se construye en tiempo lineal $\Theta(n)$ con el algoritmo de Floyd, exhibe una localidad de caché sobresaliente y entrega el máximo o mínimo en $\Theta(1)$, pero sacrifica la búsqueda eficiente de elementos arbitrarios, la cual degrada a búsqueda lineal $O(n)$.

4. **Impacto Determinante de la Ingeniería de Memoria y E/S en la JVM:**  
   En problemas de escala masiva ($N = 10^8$), la complejidad asintótica teórica debe ir acompañada de un diseño consciente del hardware y del runtime. Evitar la creación de objetos `Integer`, `String` y `Node` mediante **arrays primitivos**, **arrays paralelos** para el AVL y **lectura/escritura por bloques de bytes de 4 MB (`FastIO`)** permitió ejecutar la totalidad de los algoritmos con una huella de memoria predecible y sin pausas destructivas del Garbage Collector.

