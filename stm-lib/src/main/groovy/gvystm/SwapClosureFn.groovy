package gvystm

import clojure.lang.AFn

// TODO: can I make this work based on the number of arguments in the closure
class SwapClosureFn extends AFn {
    Closure closure;

    SwapClosureFn(Closure c) {
        closure = c
    }

    @Override
    public Object invoke(Object arg1) {
        closure(arg1)
    }
}
