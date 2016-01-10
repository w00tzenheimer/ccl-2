// Technically speaking, we could have Ast.Return appear in e.g.
// the condition of a while loop.
// This shouldn't normally happen given the way the Parser works.
// TODO: Code defensively against messed up AST.

import java.util.ArrayList;

public class Evaluator extends AstVisitor<Val> {

  // Start something like a goroutine from golang.
  // Right now, to keep implementation simple I just spawn a thread,
  // but you shouldn't count on it. Potentially, this could use a threadpool,
  // move these around in various threads like in go, etc.
  public static void go(final Val f) {
    new Thread() {
      public void run() {
        call(f, "__call__", new ArrayList<Val>());
      }
    }.start();
  }

  public static void go(Runnable r) {
    new Thread(r).start();
  }

  public final Scope scope;
  public boolean ret = false, br = false, cont = false;

  public Evaluator(Scope scope) { this.scope = scope; }

  public Val eval(Ast node) {
    return visit(node);
  }

  // Statement only

  public Val visitReturn(Ast.Return node) {
    Val val = visit(node.val);
    ret = true;
    return val;
  }

  public Val visitWhile(Ast.While node) {
    while (visit(node.cond).truthy()) {
      Val val = visit(node.body);
      if (ret) return val;
      if (br) { br = false; break; }
      if (cont) { cont = false; continue; }
    }
    return Nil.val;
  }

  public Val visitBlock(Ast.Block node) {
    for (int i = 0; i < node.body.size(); i++) {
      Val val = visit(node.body.get(i));
      if (ret||br||cont) return val;
    }
    return Nil.val;
  }

  public Val visitBreak(Ast.Break node) {
    br = true;
    return Nil.val;
  }

  public Val visitContinue(Ast.Continue node) {
    cont = true;
    return Nil.val;
  }

  // Statement or Expression

  public Val visitIf(Ast.If node) {
    return visit(visit(node.cond).truthy() ? node.body : node.other);
  }

  // Expression only

  public Val visitInt(Ast.Int node) {
    return Num.from(node.val);
  }

  public Val visitFlt(Ast.Flt node) {
    return Num.from(node.val);
  }

  public Val visitStr(Ast.Str node) {
    return Str.from(node.val);
  }

  public Val visitName(Ast.Name node) {
    Val val = scope.getOrNull(node.name);
    if (val == null) {
      Err e = new Err("Variable '" + node.name + "' not defined");
      e.add(node);
      throw e;
    }
    return val;
  }

  public Val visitAssign(Ast.Assign node) {
    Val val = visit(node.val);
    scope.put(node.name, val);
    return val;
  }

  public Val visitFunction(Ast.Function node) {
    return new UserFunc(
        node.token, node.args, node.optargs, node.vararg, node.body,
        node.newScope, scope);
  }

  public static Val call(Val owner, String name, ArrayList<Val> args) {
    if (name.equals("__call__") && owner instanceof Func)
      return ((Func) owner).call(owner, args);
    else
      return owner.call(name, args);
  }

  public Val visitCall(Ast.Call node) {
    Val owner = visit(node.owner);
    if (owner == null)
      throw new Err("FUBAR!");
    ArrayList<Val> args = new ArrayList<Val>();
    for (int i = 0; i < node.args.size(); i++)
      args.add(visit(node.args.get(i)));
    if (node.vararg != null)
      args.addAll(visit(node.vararg).as(List.class, "vararg").val);

    try { return call(owner, node.name, args); }
    catch (final Err e) { e.add(node); throw e; }
    catch (final Throwable e) { throw new Err(e); }
  }

  public Val visitGetMethod(Ast.GetMethod node) {
    // TODO: Figure out if this is really worth it.
    try { throw new Err("GetMethod not implemented"); }
    catch (final Err e) { e.add(node); throw e; }
  }

  public Val visitGetAttribute(Ast.GetAttribute node) {
    Val owner = visit(node.owner);
    Val v = owner.as(Blob.class, "self").attrs.get(node.name);
    if (v == null)
      throw new Err(
          "No attribute '" + node.name + "' for type " + owner.getMetaName());
    return v;
  }

  public Val visitSetAttribute(Ast.SetAttribute node) {
    Val v = visit(node.val);
    visit(node.owner).as(Blob.class, "self").attrs.put(node.name, v);
    return v;
  }

  public Val visitIs(Ast.Is node) {
    return visit(node.left) == visit(node.right) ? Bool.tru : Bool.fal;
  }

  public Val visitIsNot(Ast.IsNot node) {
    return visit(node.left) != visit(node.right) ? Bool.tru : Bool.fal;
  }

  public Val visitNot(Ast.Not node) {
    return visit(node.target).truthy() ? Bool.fal : Bool.tru;
  }

  public Val visitAnd(Ast.And node) {
    Val left = visit(node.left);
    return left.truthy() ? visit(node.right) : left;
  }

  public Val visitOr(Ast.Or node) {
    Val left = visit(node.left);
    return left.truthy() ? left : visit(node.right);
  }

  // Module

  public Val visitModule(Ast.Module node) {
    return visit(node.body);
  }
}
