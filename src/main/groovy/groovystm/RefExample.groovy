package groovystm

import clojure.lang.Ref;
import clojure.lang.Var;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import java.util.concurrent.*;

import static groovystm.STM.doSync
import static groovystm.STM.alter
import static groovystm.STM.refSet
import static groovystm.STM.binding
import static groovystm.STM.deref
import static groovystm.STM.ensure
import static groovystm.STM.addWatch
import static groovystm.STM.removeWatch
import static groovystm.STM.swap
import static groovystm.STM.send
import static groovystm.STM.sendOff
import static groovystm.STM.setErrorHandler
import static groovystm.STM.setErrorMode
import static groovystm.STM.restartAgent
import static groovystm.STM.agentError
import static groovystm.STM.setErrorHandler
import static groovystm.STM.errorHandler
import static groovystm.STM.setValidator
import static groovystm.STM.getValidator
import static groovystm.STM.await
import static groovystm.STM.awaitFor
import static groovystm.STM.var

/** groovy version of the example at http://clojure.org/refs 
(defn run [nvecs nitems nthreads niters]
  (let [vec-refs (vec (map (comp ref vec)
                           (partition nitems (range (* nvecs nitems)))))
        swap #(let [v1 (rand-int nvecs)
                    v2 (rand-int nvecs)
                    i1 (rand-int nitems)
                    i2 (rand-int nitems)]
                (dosync
                 (let [temp (nth @(vec-refs v1) i1)]
                   (alter (vec-refs v1) assoc i1 (nth @(vec-refs v2) i2))
                   (alter (vec-refs v2) assoc i2 temp))))
        report #(do
                 (prn (map deref vec-refs))
                 (println "Distinct:"
                          (count (distinct (apply concat (map deref vec-refs))))))]
    (report)
    (dorun (apply pcalls (repeat nthreads #(dotimes [_ niters] (swap)))))
    (report)))
*/
public class RefExample {
    int vectorCount
    int itemsPerVector
    int threadCount
    int threadIterations

    PersistentVector vecRefs = PersistentVector.EMPTY

    /**
    *   Initializes a vector of nvecs references, each containing a vector of nitems numbers 
    */
    public RefExample(int nvecs, int nitems, int nthreads, int niters) {
        vectorCount = nvecs;
        itemsPerVector = nitems;
        threadCount = nthreads;
        threadIterations = niters;

        def range = 0..<(nvecs*nitems)
        Map chunks = range.groupBy { (int)(it / nitems) }
        chunks.values().each { 
            vecRefs = vecRefs.cons(new Ref(PersistentVector.create(it)))
        }
    }

    /**
    *   Randomly swaps an item between two of the references in a transaction
    */
    public swap() {
        Random r = new Random()
        def v1 = r.nextInt(vectorCount)
        def v2 = r.nextInt(vectorCount)
        def i1 = r.nextInt(itemsPerVector)
        def i2 = r.nextInt(itemsPerVector)

        doSync {
            def temp = deref(vecRefs.nth(v1)).nth(i1)
            alter(vecRefs.nth(v1)) { v ->
                v.assocN(i1, deref(vecRefs.nth(v2)).nth(i2))
            }
            alter(vecRefs.nth(v2)) { v ->
                v.assocN(i2, temp)
            }
        }
    }

    /**
    *   Prints the value of the references as well as the sum of all the items
    */
    public report() {
        List vals = new ArrayList(vectorCount)
        for (int i=0;i<vecRefs.count();i++) {
            vals.add(deref(vecRefs.nth(i)))
        }
        println vals
        def sum = 0
        vals.each { vec -> vec.each { sum += it } }
        println "sum: " + sum
    }

    /**
    *   Executes nthreads threads, each executing swap niters times
    */
    public void run() {
        ExecutorService es = new ForkJoinPool()
        List callables = new ArrayList(threadCount)
        //for (int i=0;i<threadCount;i++) {
        for (i in 0..<threadCount) {
            callables.add(new Callable<Void>() {
                public Void call() {
                    //for (int j=0;j<threadIterations;j++) {
                    for (j in 0..<threadIterations) {
                        swap();
                    }
                }
            });
        }

        report()
        es.invokeAll(callables).each { it.get() }
        report()
    }
}
