# How VMF-Text Unparsing Works

This document explains parse → model → edit → unparse in detail: what is
preserved, what metadata is stored, how the formatter consumes it, and what
happens when you edit the model. It is the companion to
[`LEXICAL_PRESERVATION_ASSESSMENT.md`](../LEXICAL_PRESERVATION_ASSESSMENT.md)
(design history and gaps) and the runnable ladder under [`examples/`](../examples/).

Requires VMF-Text **0.2.1+** for the behaviors described here.

---

## 1. Pipeline overview

```text
ANTLR grammar (labeled)
        │
        ▼
  codegen (Gradle plugin)
        │
        ├─► typed VMF model (rule classes, properties, LexicalInfo types)
        ├─► ModelParser  (ANTLR parse → VMF objects + lexical attach)
        └─► ModelUnparser + DefaultFormatter (VMF → text)
```

At **parse** time the converter walks the ANTLR parse tree, builds VMF
`CodeElement`s, and attaches a `LexicalInfo` payload to each element.

At **unparse** time the generated unparser walks the model according to the
grammar’s alternatives and asks `DefaultFormatter` for each terminal/lexer
rule: whether an optional is present, which hidden text to emit before the
token, and what text to use for the token itself (value vs original lexeme).

Nothing in this design is a concrete syntax tree (CST). Trivia is **not**
owned by individual tokens as first-class nodes; it is an ordered slot list
on each rule instance. That is enough for library-grade round-trip and
surgical edits; it is not an IDE node model.

---

## 2. What lives on `LexicalInfo`

```text
LexicalInfo
  TriviaPiece[]      triviaPieces      // hidden text before each terminal of this rule
  OptionalState[]    optionalStates    // path-keyed optional presence
  ListShapeHint[]    listShapeHints    // codegen list geometry for splice
  OriginalLexeme[]   originalLexemes   // type-mapped spellings
  String             grammarElementPath
  CodeRange          originalRange
```

### 2.1 Trivia slots (`TriviaPiece`)

For a rule instance, parse-time hidden-channel text (and some empty pads) is
stored as an ordered list: **one slot before each terminal** of that rule’s
footprint, plus a trailing empty pad for EOF-less / end-of-rule cases.

Token **text** is not in trivia. Values are rendered from properties (or from
`OriginalLexeme` when applicable). Trivia only holds the whitespace/comments
*between* those terminals.

Example — ArrayLang `'(', INT, ',', INT, ')', EOF`:

| Slot index | Typical content | Before |
|------------|-----------------|--------|
| 0 | `""` or leading WS | `(` |
| 1 | `" "` or `""` | first value |
| 2 | `""` | `,` |
| 3 | `"  "` / `"\n "` | second value |
| 4 | `" "` | `)` |
| 5 | `""` | EOF / pad |

### 2.2 Optional presence (`OptionalState`)

Each optional grammar element records:

| Field | Meaning |
|-------|---------|
| `grammarElementPath` | Stable path in the rewritten grammar (e.g. `/r1/a0/sr0/a0/e0`) |
| `occurrenceIndex` | Per-path occurrence on this rule instance (`0`, `1`, …) |
| `present` | Whether that occurrence was present in the input |

The formatter looks up `(path, occurrenceIndex)` when unparsing an optional
terminal:

- Match found ⇒ use `present`
- Path recorded for other occurrences but not this one ⇒ **absent**
- No `optionalStates` at all:
  - parsed element (original range / trivia / grammar path) ⇒ **absent**
    (parse often omits absent optionals from the state list)
  - programmatic bare model ⇒ **present** (pretty-print)
- Wildcard state `path="*"` with `present=true` ⇒ **force all present**
  (used when null→value on a fully unrecorded optional group)

There is **no** positional `Boolean[]` list anymore.

### 2.3 List shape hints (`ListShapeHint`)

Codegen analyzes the unparser model and attaches hints so structural list
edits can splice trivia without guessing from `trivia.size()` alone:

| Field | Role |
|-------|------|
| `propertyName` | List property (`values`, `ids`, …) |
| `kind` | `PRIMITIVE_DELIMITED` or `MODEL_DELIMITED` |
| `prefixCount` / `suffixCount` | Terminals before first / after last item (fixed suffix excludes trailing `','?`) |
| `separatorCount` (`K`) | Terminals between consecutive items (`1` for `,`, `2` for `',' 'and'`, `0` for `ID (ID)*`) |
| `optionalTrailingCount` (`T`) | Optional trailing separator terminals (`1` for `','?`); present/absent from trivia size |
| `orderIndex` | Left-to-right list order on the same alternative |
| `alternativeIndex` | Top-level alternative the shape was derived from |
| `modelTyped` | Children are `CodeElement`s vs primitives |

**Primitive size:** `prefix + n + (n-1)*K + suffix` (+ `T` when trailing present and `n > 0`)  
**Model-typed parent size:** `prefix + (n-1)*K + suffix` (+ `T` likewise; values live on children)

Every top-level alternative is analyzed. When several candidates share a
property name, the converter picks the hint whose expected size matches the
actual trivia footprint (`alternativeIndex` scopes sibling multi-list math).

### 2.4 Original lexemes

Type-mapped lexer rules (e.g. `DOUBLE → Double`) store `{property, index, text, valueKey}`
at parse time. On unparse, if `String.valueOf(currentValue)` still equals
`valueKey`, the original spelling is emitted (`1` stays `1`, not `1.0`).

---

## 3. Formatter algorithm (per terminal)

For each terminal / lexer-rule step in the unparser:

1. **Optional?**  
   - Resolve `OptionalState` by `(rulePath, occurrenceCounter)` (see §2.2).  
   - If absent → mark consumed, emit nothing, do **not** advance trivia.  
   - If present (including wildcard force-all) → continue.
2. **Trivia**  
   - If `triviaPieces` is non-empty, emit `triviaPieces[counter]` and increment.  
   - If empty → **programmatic / cleared** path: ask
     `ProgrammaticSeparatorPolicy` (default: one space before lexer-rule
     tokens, none before string terminals like `,`).
3. **Token text**  
   - Prefer matching `OriginalLexeme`; else type-map / `String.valueOf`.

Root elements may emit a final trailing trivia slot after the walk.

---

## 4. Edit invalidation (what listeners do)

Generated change listeners on each `CodeElement` react to property/list
changes:

| Edit | Trivia / lexical effect |
|------|-------------------------|
| In-place primitive/string rewrite (`list.set`, non-null→non-null property) | **Keep** all trivia; invalidate that index’s original lexeme |
| Model-typed child insert/replace | **Keep** parent trivia; ensure leading space on new child if needed |
| Structural list add/remove (recognized shape) | **Splice** slots using `ListShapeHint` (or heuristic for single lists) |
| Optional null↔value | **Update** `OptionalState.present` (or default-present + trivia footprint); keep leading rule trivia |
| Unrecognized structural shape | **Clear** trivia on that element → conservative separators |

### 4.1 List splice (primitive)

Layout: `[P prefix][val][sep×K]…[val][S suffix]`

- Remove item `i`: drop the value slot and its coupling `K` separator slots
  (last remaining item drops **only** the value — prefix/suffix stay, so
  `()` can remain after emptying a parenthesized one-or-more list).
- Insert at `0`: insert value (+ `K` seps); pad so the new head is not glued
  to a previous terminal (e.g. after `;` in a multi-list rule).
- Insert into empty (`n=0`): insert a single value slot inside `P…S`.

### 4.2 List splice (model-typed)

Parent holds only brackets/commas (or bare commas + pad). Children own their
leading whitespace. `K=0` (separator-less) is a no-op on the parent.

### 4.3 Multi-list rules

Inter-list terminals (e.g. `;`) belong to the **previous** list’s suffix.
Trailing openers (`(` after `;`) belong to the **next** list’s prefix.
`computeTriviaBaseForList` sums earlier lists’ sizes so each splice is local.

---

## 5. Grammar shapes that work well

| Shape | Notes |
|-------|-------|
| `'(' t (',' t)* ')'` | Parenthesized primitive — ArrayLang |
| `t (',' t)*` | Bare primitive |
| `t ('\|' t)*` | Non-comma single-terminal sep |
| `t (',' 'and' t)*` | Multi-token sep (`K=2`) |
| `t (t)*` | Separator-less (`K=0`); prefer this over a lone `t+` for unparse |
| `'(' t (',' t)* ','? ')'` | Optional trailing comma (`T=1`) |
| Multi-alt lists | Shapes on every alt; prefer distinct properties/keywords per alt |
| Two lists + trailer/opener | `ids… ';' '(' nums… ')'` |
| Model-typed `'[' v (',' v)* ']'` | JSON arrays/objects |
| Bare model-typed `item (',' item)*` | Parent commas only |

**User recommendation (not a codegen feature):** if a shape is still
unrecognized, wrap items as model types (`value: n=INT`) so add/remove only
touches a leaf and parent trivia is not cleared.

**Prefer for separator-less lists:**

```antlr
items+=ID (items+=ID)*   // good — two labeled occurrences, K=0
// items+=ID+            // avoid — single EBNF element; unparse is unreliable
```

---

## 6. What still clears / needs a CST leap

Doable on this model and already handled where noted above. Still **out of
scope without richer per-gap or CST state**:

| Case | Why |
|------|-----|
| Separators that differ by **index** (Oxford comma: `,` then `, and`) | Needs per-gap separator kind |
| Unlabeled alt separators (`(('+'\|'-') t)*` with no property) | Choice not on the model |
| Optional separator **per gap** (`(','? item)*`) | Presence varies by index |
| Arbitrary cross-rule moves keeping outer container trivia perfectly | Trivia is rule-scoped |

See assessment § *Remaining gaps* for the CST leap boundary.

---

## 7. Programmatic models

Elements created without parsing have empty `triviaPieces` and usually empty
`optionalStates`. Unparse uses `ProgrammaticSeparatorPolicy`. After a
structural clear, the same policy applies until the model is re-parsed.

---

## 8. Source bundles vs unparse-after-edit

Source bundles store original source for persistence / restore-when-semantics-
match. They are **orthogonal** to unparse-after-edit: editing the model and
calling `unparse()` always goes through the formatter + `LexicalInfo`, not
through bundle restore.

---

## 9. How to verify

```bash
# full chain (mirrors CI)
sh ./build-and-test-all.sh

# focused lexical suite
cd test-suite
sh ./gradlew test --tests "eu.mihosoft.vmftext.tests.lexicalpreservation.*" --no-daemon

# examples (0.2.1 from Central, or mavenLocal after local publish)
cd examples/list-separators && ./gradlew run
```

Runnable demos: [`examples/README.md`](../examples/README.md).
