package test

import static gvystm.STM.doSync
import clojure.lang.Ref
import clojure.lang.PersistentHashMap;

class StatefulService {

    Ref valueMapRef = new Ref(PersistentHashMap.EMPTY);    

    static transactional = true

    def addValue(String key, String value) {
        doSync {
            Map valueMap = valueMapRef.deref();
            valueMapRef.set(valueMap.assoc(key,value));
        }
    }

    def getValue(String key) {
        valueMapRef.deref().get(key);
    }
}
