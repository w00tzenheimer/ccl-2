package com.ccl.core;

import java.util.HashMap;

public class Runtime {
  private Scope global = new Scope(null);

  protected HashMap<String, Blob> moduleRegistry =
      new HashMap<String, Blob>();

  public Runtime() {
    populateGlobalScope(global);
  }

  public void populateGlobalScope(Scope scope) {
    scope
        .put("nil", Nil.value)
        .put("true", Bool.yes)
        .put("false", Bool.no)
        .put("Value", Value.META)
        .put("Meta", Blob.META)
        .put("Bool", Bool.META)
        .put("List", List.META)
        .put("Nil", Nil.META)
        .put("Number", Number.META)
        .put("Text", Text.META)
        .put("UserFunction", UserFunction.META);
  }

  public final Scope getGlobalScope() {
    return global;
  }

  // By overriding 'makeEvaluator', subclasses can decide to use a different
  // Evaluator than the default one provided in this package.
  public Evaluator makeEvaluator(Scope scope) {
    return new Evaluator(this, scope);
  }

  public final void runMainModule(String code) {
    runModule(new Parser(code, "<main>").parse());
  }

  public final Blob runModule(Ast.Module module) {
    Scope scope = new Scope(global);
    Evaluator evaluator = makeEvaluator(scope);
    evaluator.visit(module);
    return new Blob(Blob.MODULE_META, scope.table);
  }

  public final Blob loadModule(String uri) {
    return runModule(new Parser(readModule(uri), uri).parse());
  }

  public final Blob importModule(String uri) {
    if (moduleRegistry.get(uri) == null) {
      moduleRegistry.put(uri, loadModule(uri));
    }
    return moduleRegistry.get(uri);
  }

  // In order to import modules, subclass Runtime must override
  // 'readModule'.
  public String readModule(String uri) {
    throw new Err("Importing module not supported");
  }
}