package gvystm

import clojure.lang.AFn

// TODO: can I make this work based on the number of arguments in the closure
class WatchClosureFn extends AFn {
    Closure closure;

    WatchClosureFn(Closure c) {
        closure = c
    }

    @Override
    public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
        closure(arg1, arg2, arg3, arg4)   
    }
}
