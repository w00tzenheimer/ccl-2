package com.ccl;

import java.util.ArrayList;
import java.math.BigInteger;

public final class Parser {
  public final Lexer lexer;
  public final String name;
  public final String filespec;
  public Parser(String string, String filespec) {
    this(new Lexer(string, filespec));
  }
  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.filespec = lexer.filespec;
    this.name = filespecToName(lexer.filespec);
  }
  private Token peek() { return lexer.peek; }
  private Token next() { return lexer.next(); }
  private boolean at(String type) { return peek().type.equals(type); }
  private boolean consume(String type) {
    if (at(type)) {
      next();
      return true;
    }
    return false;
  }
  private Token expect(String type) {
    if (!at(type))
      throw new SyntaxError(
          peek(), "Expected " + type + " but found " + peek().type);
    return next();
  }
  public Ast.Module parse() {

    Token token = peek();
    ArrayList<Ast.Statement> exprs = new ArrayList<Ast.Statement>();
    while (!at("EOF"))
      exprs.add(parseStatement());

    return new Ast.Module(token, name, new Ast.Block(token, exprs));
  }
  public Ast.Statement parseStatement() {

    if (at("{")) {
      Token token = next();
      ArrayList<Ast.Statement> exprs = new ArrayList<Ast.Statement>();
      while (!at("}"))
        exprs.add(parseStatement());
      expect("}");
      return new Ast.Block(token, exprs);
    }

    if (at("return")) {
      Token token = next();
      Ast.Expression value = parseExpression();
      return new Ast.Return(token, value);
    }

    if (at("while")) {
      Token token = next();
      Ast.Expression cond = parseExpression();
      Ast.Statement body = parseStatement();
      return new Ast.While(token, cond, body);
    }

    if (at("if")) {
      Token token = next();
      Ast.Expression cond = parseExpression();
      consume("then");
      Ast.Statement body = parseStatement();
      Ast.Statement other = null;
      if (consume("else"))
        other = parseStatement();
      return new Ast.If(token, cond, body, other);
    }

    if (at("break"))
      return new Ast.Break(next());

    if (at("continue"))
      return new Ast.Continue(next());

    return new Ast.ExpressionStatement(peek(), parseExpression());
  }
  public Ast.Expression parseExpression() {
    return parseOrExpression();
  }
  public Ast.Expression parseOrExpression() {
    Ast.Expression node = parseAndExpression();
    while (true) {
      if (at("or")) {
        Token token = next();
        Ast.Expression right = parseAndExpression();
        node = new Ast.Or(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parseAndExpression() {
    Ast.Expression node = parseCompareExpression();
    while (true) {
      if (at("and")) {
        Token token = next();
        Ast.Expression right = parseCompareExpression();
        node = new Ast.And(token, node, right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parseCompareExpression() {
    Ast.Expression node = parseAdditiveExpression();
    while (true) {
      if (at("==")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__eq__", right);
        continue;
      }
      if (at("!=")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__ne__", right);
        continue;
      }
      if (at("<")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__lt__", right);
        continue;
      }
      if (at("<=")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__le__", right);
        continue;
      }
      if (at(">")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__gt__", right);
        continue;
      }
      if (at(">=")) {
        Token token = next();
        Ast.Expression right = parseAdditiveExpression();
        node = new Ast.Call(token, node, "__ge__", right);
        continue;
      }
      if (at("is")) {
        Token token = next();
        if (consume("not")) {
          Ast.Expression right = parseAdditiveExpression();
          node = new Ast.IsNot(token, node, right);
        } else {
          Ast.Expression right = parseAdditiveExpression();
          node = new Ast.Is(token, node, right);
        }
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parseAdditiveExpression() {
    Ast.Expression node = parseMultiplicativeExpression();
    while (true) {
      if (at("+")) {
        Token token = next();
        Ast.Expression right = parseMultiplicativeExpression();
        node = new Ast.Call(token, node, "__add__", right);
        continue;
      }
      if (at("-")) {
        Token token = next();
        Ast.Expression right = parseMultiplicativeExpression();
        node = new Ast.Call(token, node, "__sub__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parseMultiplicativeExpression() {
    Ast.Expression node = parsePrefixExpression();
    while (true) {
      if (at("*")) {
        Token token = next();
        Ast.Expression right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__mul__", right);
        continue;
      }
      if (at("/")) {
        Token token = next();
        Ast.Expression right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__div__", right);
        continue;
      }
      if (at("//")) {
        Token token = next();
        Ast.Expression right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__floordiv__", right);
        continue;
      }
      if (at("%")) {
        Token token = next();
        Ast.Expression right = parsePrefixExpression();
        node = new Ast.Call(token, node, "__mod__", right);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parsePrefixExpression() {
    // Negative/positive numeric signs for constants must be
    // handled here because otherwise, we wouldn't be able to
    // distinguish between 'x-1' meaning 'x', '-', '1' or
    // 'x', '-1'.
    if (at("+")) {
      Token token = next();
      Ast.Expression node = parsePrefixExpression();
      return new Ast.Call(token, node, "__pos__");
    }
    if (at("-")) {
      Token token = next();
      Ast.Expression node = parsePrefixExpression();
      return new Ast.Call(token, node, "__neg__");
    }
    if (at("not")) {
      Token token = next();
      Ast.Expression node = parsePrefixExpression();
      return new Ast.Not(token, node);
    }
    return parsePostfixExpression();
  }
  public Ast.Expression parsePostfixExpression() {
    Ast.Expression node = parsePrimaryExpression();
    while (true) {
      if (at("[")) {
        Token token = expect("[");
        ArrayList<Ast.Expression> args = new ArrayList<Ast.Expression>();
        Ast.Expression vararg = null;
        while (!consume("]")) {
          if (consume("*")) {
            vararg = parseExpression();
            expect("]");
            break;
          } else {
            args.add(parseExpression());
            consume(",");
          }
        }

        if (at("=")) {
          token = next();
          if (vararg != null || args.size() != 1)
            throw new SyntaxError(
                token, "For setitem syntax, must have exactly one argument");
          node = new Ast.Call(
              token, node, "__setitem__", args.get(0), parseExpression());
        } else {
          node = new Ast.Call(token, node, "__call__", args, vararg);
        }
        continue;
      }

      if (at("@")) {
        Token token = next();
        String name = (String) expect("ID").value;
        if (at("=")) {
          token = next();
          Ast.Expression value = parseExpression();
          node = new Ast.SetAttribute(token, node, name, value);
        } else {
          node = new Ast.GetAttribute(token, node, name);
        }
        continue;
      }

      if (at(".")) {
        Token token = next();
        String name = (String) expect("ID").value;
        if (consume("[")) {
          ArrayList<Ast.Expression> args = new ArrayList<Ast.Expression>();
          Ast.Expression vararg = null;
          while (!consume("]")) {
            if (consume("*")) {
              vararg = parseExpression();
              expect("]");
              break;
            } else {
              args.add(parseExpression());
              consume(",");
            }
          }
          node = new Ast.Call(token, node, name, args, vararg);
        }
        else
          node = new Ast.GetMethod(token, node, name);
        continue;
      }
      break;
    }
    return node;
  }
  public Ast.Expression parsePrimaryExpression() {

    if (at("STR")) {
      Token token = next();
      return new Ast.Str(token, (String) token.value);
    }

    if (at("FLT")) {
      Token token = next();
      return new Ast.Flt(token, (Double) token.value);
    }

    if (at("INT")) {
      Token token = next();
      return new Ast.Int(token, (BigInteger) token.value);
    }

    if (at("ID")) {
      Token token = next();
      String name = (String) token.value;

      if (at("=")) {
        token = next();
        Ast.Expression value = parseExpression();
        return new Ast.Assign(token, new Ast.NamePattern(name), value);
      } else {
        return new Ast.Name(token, name);
      }
    }

    if (consume("(")) {
      Ast.Expression expr = parseExpression();
      expect(")");
      return expr;
    }

    if (at("\\") || at("\\\\")) {
      boolean newScope = at("\\");
      Token token = next();
      Ast.ListPattern args = parseListPattern();
      if (!consume(".") && !at("{"))
        throw new SyntaxError(
            peek(),
            "Expected either a '.' or '{' to indicate " +
            "the end of the argument list.");
      Ast.Statement body = parseStatement();
      return new Ast.Function(token, args, body, newScope);
    }

    if (at("if")) {
      Token token = next();
      Ast.Expression cond = parseExpression();
      consume("then");
      Ast.Expression body = parseExpression();
      expect("else");
      Ast.Expression other = parseExpression();
      return new Ast.Ternary(token, cond, body, other);
    }

    if (at("let")) {
      Token token = next();
      Ast.Pattern pattern = parsePattern();
      expect("=");
      Ast.Expression val = parseExpression();
      return new Ast.Assign(token, pattern, val);
    }

    throw new SyntaxError(
        peek(), "Expected expression but found " + peek().type);
  }

  public Ast.Pattern parsePattern() {
    if (consume("[")) {
      Ast.Pattern pattern = parseListPattern();
      expect("]");
      return pattern;
    }
    return new Ast.NamePattern((String) expect("ID").value);
  }

  public Ast.ListPattern parseListPattern() {
    ArrayList<Ast.Pattern> args = new ArrayList<Ast.Pattern>();
    while (at("ID") || at("[")) {
      args.add(parsePattern());
      consume(",");
    }
    ArrayList<Ast.Pattern> optargs = new ArrayList<Ast.Pattern>();
    while (consume("/")) {
      optargs.add(parsePattern());
      consume(",");
    }
    String vararg = null;
    if (consume("*"))
      vararg = (String) expect("ID").value;
    return new Ast.ListPattern(args, optargs, vararg);
  }

  public static String filespecToName(String filespec) {
    int start, end = filespec.length();
    for (start = filespec.length()-1;
          start >= 1 && filespec.charAt(start-1) != '/' &&
          filespec.charAt(start-1) != '\\'; start--);
    if (filespec.endsWith(".ccl"))
      end -= ".ccl".length();
    return filespec.substring(start, end);
  }

}