/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmftext.tests.expressionlangplus;


import java.util.HashMap;
import java.util.Map;

class GrammarInterpreter {
    // variable map
    private Map<String, Expr> variables;

    public Map<String, Expr> execute(ExpressionLangPlusModel model) {
        // init variable map
        variables = new HashMap<>();

        // run the interpreter
        try {
            model.getRoot().getExpressions().forEach(expr -> toValue(expr));
        } catch(Exception ex) {
            //
        }

        // print all declared variables
        for(String varName : variables.keySet()) {
            Expr valueExpr = variables.get(varName);
            String valueString = null;
            if(valueExpr instanceof NumberExpr) {
                valueString = "number "+ ((NumberExpr) valueExpr).getValue();
            } else if(valueExpr instanceof ArrayExpr) {
                valueString = "array "+ valueExpr.toString();
            }
            System.out.println(" -> var\t'" + varName + "'\t\t" + valueString);
        }

        return variables;
    }

    NumberExpr toValue(Expr e) {
        if(e instanceof MethodCallExpr) {
            return toValue((MethodCallExpr)e);
        } else if(e instanceof OpExpr) {
            OpExpr opExpr = (OpExpr) e;
            return toValue(opExpr);
        } else if(e instanceof VariableExpr) {
            VariableExpr variableExpr = (VariableExpr) e;
            if(!variables.containsKey(variableExpr.getVariable())) {
                String msg = "Error in [line " + variableExpr.getCodeRange().getStart().getLine()+", column "+
                        variableExpr.getCodeRange().getStart().getCharPosInLine()
                        + "]: usage of undeclared variable '" + variableExpr.getVariable() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return toValue(variables.get(variableExpr.getVariable()));
        } else if(e instanceof AssignmentExpr) {
            AssignmentExpr assignmentExpr = (AssignmentExpr) e;

            Expr rExpr = assignmentExpr.getExpression();

            if (rExpr instanceof ArrayExpr) {
                variables.put(assignmentExpr.getVariable(), rExpr);
            } else {
                variables.put(assignmentExpr.getVariable(), toValue(rExpr));
            }

            return toValue(rExpr);
        } else if(e instanceof ArrayIndexExpr) {
            ArrayIndexExpr arrayIndexExpr = (ArrayIndexExpr) e;

            if(!variables.containsKey(arrayIndexExpr.getVariable())) {
                String msg = "Error in [line " + arrayIndexExpr.getCodeRange().getStart().getLine()+", column "+
                        arrayIndexExpr.getCodeRange().getStart().getCharPosInLine()
                        + "]: usage of undeclared variable '" + arrayIndexExpr.getVariable() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }

            Expr arrExpr = variables.get(arrayIndexExpr.getVariable());

            if(!(arrExpr instanceof ArrayExpr)) {

                String msg = "Error in [line " + arrayIndexExpr.getCodeRange().getStart().getLine()+", column "+
                        arrayIndexExpr.getCodeRange().getStart().getCharPosInLine()
                        + "]: variable '" + arrayIndexExpr.getVariable() + "' is not of type array.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }

            ArrayExpr arrayExpr = (ArrayExpr) arrExpr;

            if(arrayIndexExpr.getIndex() >= arrayExpr.getArrayValue().getElements().size() || arrayIndexExpr.getIndex() < 0) {
                String msg = "Error in [line " + arrayIndexExpr.getCodeRange().getStart().getLine()+", column "+
                        arrayIndexExpr.getCodeRange().getStart().getCharPosInLine()
                        + "]: array index out of bounds: size of '" + arrayIndexExpr.getVariable()
                        + "'=" + arrayExpr.getArrayValue().getElements().size()
                        + ", requested index = " + arrayIndexExpr.getIndex() + ".";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }

            Double arrayValue = arrayExpr.getArrayValue().getElements().get(arrayIndexExpr.getIndex());

            return toValue(NumberExpr.newBuilder().withValue(arrayValue).build());
        } else if(e instanceof NumberExpr) {
            return (NumberExpr) e;
        } else if(e instanceof ParanExpr) {
            return toValue(((ParanExpr)e).getExpression());
        } else if(e instanceof ArrayExpr) {
            return null;
        }

        String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                e.getCodeRange().getStart().getCharPosInLine()
                + "]: expression is not a value" + ".";
        System.err.println(msg);
        throw new RuntimeException(msg);

    }

    NumberExpr toValue(OpExpr e) {

        NumberExpr left = toValue(e.getLeft());
        NumberExpr right = toValue(e.getRight());

        if(left == null) {
            String msg = "Error in [line " + e.getLeft().getCodeRange().getStart().getLine()+", column "+
                    e.getLeft().getCodeRange().getStart().getCharPosInLine()
                    + "]: cannot apply operator '" + e.getOperator() + "'. Left expression is not a value" + ".";
            System.err.println(msg);
            throw new RuntimeException(msg);
        }

        if(right == null) {
            String msg = "Error in [line " + e.getLeft().getCodeRange().getStart().getLine()+", column "+
                    e.getLeft().getCodeRange().getStart().getCharPosInLine()
                    + "]: cannot apply operator '" + e.getOperator() + "'. Right expression is not a value" + ".";
            System.err.println(msg);
            throw new RuntimeException(msg);
        }

        if("+".equals(e.getOperator())) {
            return NumberExpr.newBuilder().withValue(
                    left.getValue() + right.getValue()
            ).build();
        } else if("-".equals(e.getOperator())) {
            return NumberExpr.newBuilder().withValue(
                    left.getValue() - right.getValue()
            ).build();
        } else if("*".equals(e.getOperator())) {
            return NumberExpr.newBuilder().withValue(
                    left.getValue() * right.getValue()
            ).build();
        } else if("/".equals(e.getOperator())) {
            return NumberExpr.newBuilder().withValue(
                    left.getValue() / right.getValue()
            ).build();
        }

        return null;
    }

    NumberExpr toValue(MethodCallExpr e) {
        if("sin".equals(e.getName())) {
            if (e.getParams().getElements().size()!=1) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.sin(toValue(e.getParams().getElements().get(0)).getValue())).build();
        } else if ("cos".equals(e.getName())) {
            if (e.getParams().getElements().size()!=1) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.cos(toValue(e.getParams().getElements().get(0)).getValue())).build();
        } else if ("sqrt".equals(e.getName())) {
            if (e.getParams().getElements().size()!=1) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.sqrt(toValue(e.getParams().getElements().get(0)).getValue())).build();
        } else if ("abs".equals(e.getName())) {
            if (e.getParams().getElements().size()!=1) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.abs(toValue(e.getParams().getElements().get(0)).getValue())).build();
        } else if ("min".equals(e.getName())) {
            if (e.getParams().getElements().size()!=2) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.min(
                    toValue(e.getParams().getElements().get(0)).getValue(),
                    toValue(e.getParams().getElements().get(1)).getValue())).build();
        }  else if ("max".equals(e.getName())) {
            if (e.getParams().getElements().size()!=2) {
                String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                        e.getCodeRange().getStart().getCharPosInLine()
                        + "]: wrong number of parameters for method '" + e.getName() + "'.";
                System.err.println(msg);
                throw new RuntimeException(msg);
            }
            return NumberExpr.newBuilder().withValue(Math.max(
                    toValue(e.getParams().getElements().get(0)).getValue(),
                    toValue(e.getParams().getElements().get(1)).getValue())).build();
        } else {
            String msg = "Error in [line " + e.getCodeRange().getStart().getLine()+", column "+
                    e.getCodeRange().getStart().getCharPosInLine()
                    + "]: usage of unknown method '" + e.getName() + "'.";
            System.err.println(msg);
            throw new RuntimeException(msg);
        }
    }
}

