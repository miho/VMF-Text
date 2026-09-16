# Parser Rule Maps (`RuleMap`)

Issue [#1](https://github.com/miho/VMF-Text/issues/1).

`RuleMap` flattens a **single-alternative wrapper parser rule** at the places it
is referenced, so the generated model exposes the wrapped target type directly
instead of the intermediate wrapper. It is the parser-rule counterpart of the
lexer-token [`TypeMap`](../README.md).

## Syntax

Declared in a `/*<!vmf-text!> ... */` comment block, alongside `TypeMap`:

```antlr
RuleMap() {
  (first: SourceType -> second: TargetType) = {
      'source-to-target expression',   // parse:   'first'  is the source object
      'target-to-source expression'    // unparse: 'second' is the target object
  }
}
```

- `SourceType` / `TargetType` are **generated model type names** (PascalCase).
- The parse expression receives the fully built source model object bound to
  `first` and returns the target.
- The unparse expression receives the target bound to `second` and returns a
  reconstructed source object.
- `RuleMap(RuleA, RuleB)` scopes the map to properties on the named container
  rules; `RuleMap()` (empty) applies globally.

## Example

```antlr
program: (expressions+=expression ';')*;
expression: value = numberLiteral # valueExpression;
numberLiteral: value = INT # intLiteral | value = DOUBLE # doubleLiteral;

/*<!vmf-text!>
RuleMap() {
  (first: ValueExpression -> second: NumberLiteral) = {
      'first.getValue()',
      'ValueExpression.newBuilder().withValue(second).build()'
  }
}
*/
```

Without the map, `Program.getExpressions()` returns `List<Expression>`. With it
the wrapper is flattened and `Program.getExpressions()` returns
`List<NumberLiteral>` directly. Parsing `1;2;3;` yields three `NumberLiteral`s;
unparsing reproduces the source. (Runnable version:
`test-suite/src/main/vmf-text/eu/mihosoft/vmftext/tests/rulemap/RuleMap.g4`.)

## How it works

1. **Model redirect** — a post-model pass (`RuleMapModelRewriter`) retypes each
   rule-typed property whose type is the source to the target type.
2. **Parse** — the generated converter builds the source object, evaluates the
   source→target expression (`first`), and stores the target on the parent.
3. **Unparse** — the generated unparser reconstructs the source object from the
   target (`second`) and unparses *that*, so the emitted text is a valid instance
   of the original grammar rule (this also satisfies the unparser's match-alt
   validation, which still references the original rule).

## Resolution rules

The source may be named directly, or be the **single concrete subclass** of a
base rule that a property references (the common single-labeled-alternative
case). Polymorphic base rules with more than one concrete subclass are left
untouched, because redirecting them would be ambiguous — reference the concrete
rule or split the alternatives.

## Lexical preservation

Round-trip is **byte-exact for unedited parsed models**, including both
transparent wrappers and token-bearing wrappers:

- **Transparent wrappers** (e.g. `expression: value = numberLiteral`) — the
  wrapped target subtree keeps its own parse-time trivia and separators owned
  by the parent rule survive, so an unedited parse/unparse reproduces the input
  exactly (verified by `RuleMapTest.roundTripIsByteExact`).
- **Token-bearing wrappers** (e.g. `expr: '(' value=numberLiteral ')'`) — the
  discarded wrapper's ANTLR context is captured as a shell `LexicalInfo`
  sidecar on the flattened target (`CodeElement.getRuleMapShellLexicalInfo()`).
  On unparse the shell is restored onto the reconstructed source so
  wrapper-owned terminals and trivia round-trip byte-exactly (verified by
  `RuleMapTokenBearingTest.roundTripIsByteExact`).

**Boundary.** Programmatic targets (or any target whose shell LexicalInfo was
cleared by a semantic edit) have no shell to restore; unparse reconstructs the
wrapper with the formatter's conservative separators. That output is still
valid grammar text, but not guaranteed byte-exact relative to a prior source
span.
