import parse


class TranslationError(parse.ParseError):
  pass


def TranslateModule(node, seen=None):
  if seen is None:
    seen = set()

  translation = ''

  for include in node.includes:
    if include.uri not in seen:
      seen.add(include.uri)
      content = GetContentFromInclude(include)
      translation += TranslateModule(parse.Parse(content, include.uri), seen)

  translation += TranslateStatement(node, 0)

  return translation


def GetContentFromInclude(include):
  raise TranslationError('include not yet supported', include.origin)


def TranslateStatement(node, depth):
  if isinstance(node, parse.Module):
    return '  ' * depth + ''.join(TranslateStatement(cls, depth) for cls in node.classes)
  elif isinstance(node, parse.Class):
    bases = ['CC' + base for base in node.bases]
    if not bases:
      bases.append('CCObject')
    return '\n' + '  ' * depth + 'class CC' + node.name + '(' + ', '.join(bases) + '):\n' + (''.join(TranslateStatement(method, depth+1) for method in node.methods) or ('  ' * (depth+1) + 'pass\n'))
  elif isinstance(node, parse.Method):
    args = ['XX' + arg for arg, _ in node.arguments]
    return '\n  ' * depth + 'def MM' + node.name + '(XXthis' + ''.join(', ' + arg for arg in args) + '):\n' + TranslateStatement(node.body, depth+1)
  elif isinstance(node, parse.Declaration):
    return ''
  elif isinstance(node, parse.StatementBlock):
    translation = ''.join(TranslateStatement(s, depth) for s in node.statements)
    return translation or ('  ' * depth + 'pass\n')
  elif isinstance(node, parse.Return):
    return '  ' * depth + 'return ' + TranslateExpression(node.expression) + '\n'
  elif isinstance(node, parse.If):
    translation = '  ' * depth + 'if ' + TranslateExpression(node.test) + ':\n' + TranslateStatement(node.body, depth+1)
    if node.other:
      translation += 'else:\n' + TranslateStatement(node.other, depth+1)
    return translation
  elif isinstance(node, parse.While):
    return '  ' * depth + 'while ' + TranslateExpression(node.test) + ':\n' + TranslateStatement(node.body, depth+1)
  elif isinstance(node, parse.Break):
    return '  ' * depth + 'break\n'
  elif isinstance(node, parse.Expression):
    return '  ' * depth + TranslateExpression(node) + '\n'

  raise TypeError('Unrecognized node type %s' % type(node))


def TranslateExpression(node):
  if isinstance(node, parse.Number):
    return 'CCNumber(%r)' % node.value
  elif isinstance(node, parse.String):
    return 'CCString(%r)' % node.value
  elif isinstance(node, parse.VariableLookup):
    return 'XX' + node.name
  elif isinstance(node, parse.New):
    return 'CC%s(%s)' % (node.class_, ', '.join(map(TranslateExpression, node.arguments)))
  elif isinstance(node, parse.GetAttribute):
    return '%s.AA%s' % (TranslateExpression(node.owner), node.attribute)
  elif isinstance(node, parse.SetAttribute):
    return '%s.AA%s = %s' % (TranslateExpression(node.owner), node.attribute, TranslateExpression(node.value))
  elif isinstance(node, parse.MethodCall):
    return '%s.MM%s(%s)' % (TranslateExpression(node.owner), node.attribute, ', '.join(map(TranslateExpression, node.arguments)))
  elif isinstance(node, parse.Assignment):
    return 'XX%s = %s' % (node.name, TranslateExpression(node.value))
  elif isinstance(node, parse.List):
    return 'CCList([%s])' % ', '.join(map(TranslateExpression, node.items))

  raise TypeError('Unrecognized node type %s' % type(node))


translation = TranslateStatement(parse.Parse(r"""

class Main

  method Run(universe)
    var world

    world = new SimplifiedUniverse(universe)

    this.Hello()
    world.Print(this)
    world.Print("hello world!")

  method Hello()
    pass

""", '<test>'), 0)


class CCObject(object):
  pass


class CCNumber(CCObject):

  def __init__(self, value):
    self.value = value

  def MMAdd(self, other):
    return self.value + other.value


class CCString(CCObject):

  def __init__(self, value):
    self.value = value

  def __str__(self):
    return self.value


class CCList(CCObject):

  def __init__(self, value):
    self.value = value


class CCUniverse(CCObject):
  pass


class CCSimplifiedUniverse(CCObject):

  def __init__(self, universe):
    self.universe = universe

  def MMPrint(self, string):
    print(string)


# print(translation)
# exec(translation + "\nCCMain().MMRun(CCUniverse())\n")


fn = 'lex.ccl'

with open(fn) as f:
  translation = TranslateModule(parse.Parse(f.read(), fn))

print(translation)
# exec(translation + 'CCMain().Run(CCUniverse())')
