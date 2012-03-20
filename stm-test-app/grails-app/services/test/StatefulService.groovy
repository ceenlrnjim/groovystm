package test

import static gvystm.STM.doSync
import static gvystm.STM.alter
import clojure.lang.Ref
import clojure.lang.PersistentHashMap;

class StatefulService {

    Ref valueMapRef = new Ref(PersistentHashMap.EMPTY);    

    static transactional = true

    def addValue(String key, String value) {
        doSync {
            alter(valueMapRef) { m -> m.assoc(key,value) }
        }
    }

    def getValue(String key) {
        valueMapRef.deref().get(key);
    }
}
