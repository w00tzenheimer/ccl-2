package com.ccl;

public class Modules extends Runtime {

  public static void main(String[] args) {
    import_test();
  }
  
  private static Scope module_test = null;
  public static Value import_test() {
    if (module_test == null) {
      module_test = new Scope(GLOBAL);
      run_test(module_test);
    }
    return module_test;
  }
  private static void run_test(Scope scope) {
    Value condition;
    {
      scope.getvar("print").call(nil, new Value[]{Str.from("hello world!")}, null);
    }
  }
}
