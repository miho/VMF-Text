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
//package eu.mihosoft.vmftext.tests.miniclang;
//
//import eu.mihosoft.vcollections.VList;
//import eu.mihosoft.vmf.runtime.core.VIterator;
//import eu.mihosoft.vmf.runtime.core.VObject;
//import eu.mihosoft.vmftext.tests.miniclang.*;
//import eu.mihosoft.vmftext.tests.miniclang.parser.MiniClangModelParser;
//import eu.mihosoft.vmftext.tests.miniclang.unparser.*;
//import eu.mihosoft.vmftext.tests.miniclang.unparser.Formatter;
//import eu.mihosoft.vtcc.interpreter.CInterpreter;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class Main {
//
//    // types used for function signatures
//    private static final Type boolType = Type.newBuilder().withTypeName("bool").build();
//    private static final Type intType = Type.newBuilder().withTypeName("int").build();
//    private static final Type doubleType = Type.newBuilder().withTypeName("double").build();
//    private static final Type stringType = Type.newBuilder().withTypeName("const char*").build();
//    private static final Type uintptrType = Type.newBuilder().withTypeName("uintptr_t").build();
//
//    public static void main(String[] args) throws IOException {
//
//        Files.list(Paths.get("samples")).map(p->p.toFile()).filter(f->!f.isDirectory()).
//                filter(f->f.getName().toLowerCase().endsWith(".c")).forEach(f-> {
//
//            try {
//                System.out.println("--------------------------------------------------------------------------------");
//                System.out.println("> "+ f.getName());
//                System.out.println("--------------------------------------------------------------------------------");
//                System.out.println(" -> parsing: " + f.getName());
//
//                // parse code to instrument
//                MiniClangModel model = parse(f);
//
//                ReadOnlyMiniClangModel instrumentedModel =
//                        instrument(model.asReadOnly(), "transpose").asReadOnly();
//
//                // run instrumented c code & collect memory trace
//                String trace = run(instrumentedModel);
//
//                //if(f.getName().contains("access")) {
//                    //System.out.println(unparse(instrumentedModel));
//                //}
//
//                String outCode = unparse(instrumentedModel);
//
//                Files.write(new File("out",f.getName()+"_out.c").toPath(), outCode.getBytes() );
//
//                // perform a cache simulation
//                CacheSim sim = new CacheSim();
//                sim.setVerbose(f.getName().contains("access"));
//                CacheSim.SimulationResult simResult = sim.execute(2,1,5, trace);
//                System.out.println(" -> result:  "+simResult);
//
//                if(f.getName().contains("access")) {
//                    System.out.println(trace);
//                    for(CacheSim.Entry e : simResult.getTrace()) {
//                        System.out.println(e.getType()+" -> "+ e.getLabel());
//                    }
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        });
//
//        MiniClangModel model = parse(
//                "void test() {for(int i = 0; i < 10;i++) {}}" +
//                "int main(int argc, char *argv[]) {\n" +
//                "  return 0;\n" +
//                "}\n" +
//                "\n");
//
//        MiniClangModel instrumentedModel = instrument(model.asReadOnly(), "test");
//
//        MiniClangModelUnparser up = new MiniClangModelUnparser();
//        up.setFormatter(new MyFormatter());
//
//        System.out.println(up.unparse(instrumentedModel));
//    }
//
//    public static CacheSim.SimulationResult runModel(String code, String fName, boolean verbose) {
//        // parse code to instrument
//        MiniClangModel model = parse(code);
//
//        ReadOnlyMiniClangModel instrumentedModel =
//                instrument(model.asReadOnly(), fName).asReadOnly();
//
//        if(verbose) {
//            MiniClangModelUnparser up = new MiniClangModelUnparser();
//            up.setFormatter(new MyFormatter());
//
//            System.out.println(up.unparse(instrumentedModel.asModifiable()));
//        }
//
//        // run instrumented c code & collect memory trace
//        String trace = run(instrumentedModel);
//
//        if(verbose) {
//
//            String[] lines = trace.split("\\R");
//
//            for(String l : lines) {
//                if(l.startsWith("=="))
//                System.out.println(l);
//            }
//        }
//
//        // perform a cache simulation
//        CacheSim sim = new CacheSim();
//        sim.setVerbose(verbose);
//        return sim.execute(2,1,5, trace);
//    }
//
//    public static String getInstrumentedCode(String code) {
//        // parse code to instrument
//        MiniClangModel model = parse(code);
//
//        ReadOnlyMiniClangModel instrumentedModel =
//                instrument(model.asReadOnly(), "transpose").asReadOnly();
//
//
//        String instrumentedCode = unparse(instrumentedModel);
//
//        return instrumentedCode;
//    }
//
//    public static MiniClangModel parse(String code) {
//
//        // parse code to instrument
//        MiniClangModelParser parser = new MiniClangModelParser();
//        MiniClangModel model = parser.parse(code);
//
//        return model;
//    }
//
//    public static MiniClangModel parse(File f) throws IOException {
//
//        // parse code to instrument
//        MiniClangModelParser parser = new MiniClangModelParser();
//        MiniClangModel model = parser.parse(f);
//
//        return model;
//    }
//
//    /**
//     * Instruments the specified function of a given model.
//     *
//     * @param model model to instrument
//     * @param fName function to instrument
//     */
//    public static void instrument(MiniClangModel model, String fName) {
//        // convert loops & if-statements with just a single statement to block statements
//        convertStatementsToBlocks(model);
//
//        // introduce ids for all grammar elements that implement 'WithId'
//        int[] idCounter = new int[1];
//        model.vmf().content().stream(WithId.class).forEach(e -> e.setId(idCounter[0]++));
//
//        FunctionDecl fDecl = model.getRoot().getFunctions().stream().
//                filter(f->fName.equals(f.getFunctionName())).findAny().get();
//
//        // add 'enter scope' & 'exit scope' instrumentation
//        // instrumentBlockEntryAndExit(fDecl);
//
//        addReadAndWriteVarInstrumentationInvocations(model,fDecl);
//        addReadAndWriteArrayVarInstrumentationInvocations(model,fDecl);
//
//        addReadAndWriteVarInstrumentationFunctions(model);
//    }
//
//    /**
//     * Instruments the specified function of a given model. Returns an instrumented copy of the specified model.
//     *
//     * @param model model to instrument
//     * @param fName function to instrument
//     *
//     * @return an instrumented version of the specified model
//     */
//    public static MiniClangModel instrument(ReadOnlyMiniClangModel model, String fName) {
//
//        MiniClangModel result = model.asModifiable();
//
//        instrument(result, fName);
//
//        return result;
//    }
//
//    public static String unparse(ReadOnlyMiniClangModel model) {
//        MiniClangModelUnparser unparser = new MiniClangModelUnparser();
//        unparser.setFormatter(new MyFormatter());
//
//        String instrumentedCode = unparser.unparse(model.asModifiable());
//
//        return instrumentedCode;
//    }
//
//    /**
//     * Runs the specified model and collects standard output.
//     *
//     * @param model model to execute
//     * @return standard output of the executed program
//     */
//    public static String run(ReadOnlyMiniClangModel model) {
//
//        String instrumentedCode = unparse(model);
//
//        // run instrumented c code
//        StringPrintStream stringPrintStream = StringPrintStream.newInstance();
//        int exitValue = CInterpreter.execute(instrumentedCode).print(
//                stringPrintStream,System.err).waitFor().getProcess().exitValue();
//
//        System.out.println(" -> exit value: " + exitValue);
//
//        // TODO what to do for invalid programs?
//        String trace = stringPrintStream.toString();
//
//        return trace;
//    }
//
//    /**
//     * Adds read-/write-array instrumentations to the specified model.
//     *
//     * @param model model to instrument
//     */
//    private static void addReadAndWriteArrayVarInstrumentationInvocations(MiniClangModel model, FunctionDecl fDecl) {
//        VIterator arrayAccessIt = fDecl.vmf().content().iterator();
//        while (arrayAccessIt.hasNext()) {
//            VObject obj = arrayAccessIt.next();
//
//            if (obj instanceof ArrayAccessExpression) {
//                addReadArrayInstrumentation(model, arrayAccessIt, (ArrayAccessExpression) obj);
//            } else if(obj instanceof ArrayAssignmentStatement) {
//                addWriteArrayInstrumentation(arrayAccessIt, (ArrayAssignmentStatement)obj);
//            }
//
//        }
//    }
//
//    private static boolean isConstant(MiniClangModel model, IdentifierExpression ie) {
//        return model.getRoot().getConstants().stream().anyMatch(cD -> Objects.equals(ie.getVarName(), cD.getVarName()));
//    }
//
//    /**
//     * Adds read-/write-var instrumentations to the specified model.
//     *
//     * @param fDecl function declaration to instrument
//     */
//    private static void addReadAndWriteVarInstrumentationInvocations(MiniClangModel model, FunctionDecl fDecl) {
//        VIterator arrayAccessIt = fDecl.vmf().content().iterator();
//        while (arrayAccessIt.hasNext()) {
//            VObject obj = arrayAccessIt.next();
//            if (obj instanceof IdentifierExpression) {
//                IdentifierExpression iE = (IdentifierExpression) obj;
//
//                VObject parent = iE.vmf().content().referencedBy().get(0);
//
//                if (!(parent instanceof ArrayAccessExpression) &&
//                        !(parent instanceof FunctionCallExpression) &&
//                        !(isConstant(model,iE))) {
//
//                    addReadVarInstrumentation(model, arrayAccessIt, (IdentifierExpression) obj, true);
//                }
//            } else if(obj instanceof VariableAssignmentStatement) {
//                addWriteVarInstrumentation(model, arrayAccessIt, (VariableAssignmentStatement)obj);
//            }
//        }
//    }
//
//    /**
//     * Adds read-var instrumentations to the specified iterator.
//     *
//     * @param arrayAccessIt iterator (may be null if modification is not intended)
//     * @param ie identifier expression to instrument
//     * @param skipArrayReads defines whether to skip array-reads
//     * @return function call
//     */
//    private static FunctionCallExpression addReadVarInstrumentation(MiniClangModel model, VIterator arrayAccessIt,
//                                                                    IdentifierExpression ie, boolean skipArrayReads) {
//        String varName = "var:" + ie.getVarName();
//        Optional<DeclStatement> varDeclStatement = ie.parentScopes().get(0).
//                resolveVariable(ie.getVarName());
//
//        Type varType;
//
//        Optional<ConstantDef> constantDefPresent = model.getRoot().getConstants().stream().
//                filter(cd->Objects.equals(ie.getVarName(),cd.getVarName())).
//                findAny();
//
//        if(constantDefPresent.isPresent()) {
//            varType = Type.newBuilder().withTypeName("int").build();
//        } else {
//            varType  = varDeclStatement.map(vD -> vD.getDeclType()).
//                    orElse(Type.newBuilder().withTypeName("void").build());
//        }
//
//        if(varDeclStatement.isPresent() && skipArrayReads) {
//            if (varDeclStatement.get().getArraySizes().size()>0) return null;
//        }
//
//        String functionName = "__vmf_r_" + varType.getTypeName() + "_d_0";
//
//        List<Expression> args = new ArrayList<>();
//
//        Expression label = StringExpression.newBuilder().withValue(computeLabel(ie)).build();
//        args.add(label);
//
//        Expression iE = StringExpression.newBuilder().withValue(varName).build();
//        args.add(iE);
//        args.add(ie);
//
//        Expression paren = ParenExpression.newBuilder().withParanExpr(ie).build();
//
//        Expression iAE = AddressOperator.newBuilder().withOperatorExpression(paren).build();
//
//        args.add(CastOperatorExpression.newBuilder().
//                withCastType(uintptrType.clone()).withOperatorExpression(iAE).build());
//
//        FunctionCallExpression fEx = FunctionCallExpression.newBuilder().
//                withFunctionName(functionName).
//                withArgs(VList.newInstance(args)).
//                build();
//
//        if(arrayAccessIt!=null) {
//            arrayAccessIt.set(fEx);
//        }
//
//        return fEx;
//    }
//
//    /**
//     * Adds a write-var instrumentation to the specified iterator.
//     *
//     * @param arrayAccessIt iterator to modify
//     * @param varAss variable assignment to instrument
//     */
//    private static void addWriteVarInstrumentation(MiniClangModel model,
//                                                       VIterator arrayAccessIt, VariableAssignmentStatement varAss) {
//        String varName = varAss.getVarName();
//
//        Type varType  = varAss.getDeclType();
//
//        if(varType==null) {
//            Optional<DeclStatement> varDeclStatement = varAss.parentScopes().get(0).
//                    resolveVariable(varName);
//            varType  = varDeclStatement.map(vD -> vD.getDeclType()).
//                    orElse(Type.newBuilder().withTypeName("void").build());
//        }
//
//        String functionName = "__vmf_w_" + varType.getTypeName() + "_d_0";
//
//        List<Expression> args = new ArrayList<>();
//
//        Expression label = StringExpression.newBuilder().withValue(computeLabel(varAss)).build();
//        args.add(label);
//        Expression vNe = StringExpression.newBuilder().withValue("var:" + varName).build();
//        args.add(vNe);
//        IdentifierExpression ie = IdentifierExpression.newBuilder().withVarName(varName).build();
//        args.add(ie);
//
//        Expression paren = ParenExpression.newBuilder().withParanExpr(ie).build();
//
//        Expression iAe = AddressOperator.newBuilder().withOperatorExpression(paren).build();
//
//        args.add(CastOperatorExpression.newBuilder().
//                withCastType(uintptrType.clone()).withOperatorExpression(iAe).build());
//
//        FunctionCallStatement fEx = FunctionCallStatement.newBuilder().
//                withFunctionName(functionName).
//                withArgs(VList.newInstance(args)).
//                build();
//
//        arrayAccessIt.add(fEx);
//    }
//
//    /**
//     * Adds a read-var instrumentation to the specified iterator.
//     *
//     * @param model model to instrument
//     * @param arrayAccessIt iterator to modify
//     * @param aeE array access expression to instrument
//     */
//    private static void addReadArrayInstrumentation(MiniClangModel model,
//                                                    VIterator arrayAccessIt, ArrayAccessExpression aeE) {
//
//        Expression variable = aeE.getArrayVariableExpression();
//
//        String varName = "";
//
//        Type varType = Type.newBuilder().withTypeName("void").build();
//
//        if (variable instanceof IdentifierExpression) {
//            IdentifierExpression ie = (IdentifierExpression) variable;
//            varName = "var:" + ie.getVarName();
//
//            Optional<DeclStatement> varDeclStatement = ie.parentScopes().get(0).
//                    resolveVariable(ie.getVarName());
//
//            Optional<ConstantDef> constantDefPresent = model.getRoot().getConstants().stream().
//                    filter(cd->Objects.equals(ie.getVarName(),cd.getVarName())).
//                    findAny();
//
//            if(constantDefPresent.isPresent()) {
//                varType = Type.newBuilder().withTypeName("int").build();
//            } else {
//                varType  = varDeclStatement.map(vD -> vD.getDeclType()).
//                        orElse(Type.newBuilder().withTypeName("void").build());
//            }
//
//        } else if (variable instanceof FunctionCallExpression) {
//            FunctionCallExpression fcE = (FunctionCallExpression) variable;
//            varName = "fcall:" + fcE.getFunctionName();
//            Optional<FunctionDecl> functionDecl = model.getRoot().
//                    resolveFunction(fcE.getFunctionName(), fcE.getArgs().size());
//            varType = functionDecl.map(fD -> fD.getReturnType()).
//                    orElse(Type.newBuilder().withTypeName("void").build());
//        }
//
//        String functionName = "__vmf_r_" + varType.getTypeName() + "_d_" + aeE.getArrayIndices().size();
//
//        List<Expression> args = new ArrayList<>();
//
//        Expression label = StringExpression.newBuilder().withValue(computeLabel(aeE)).build();
//        args.add(label);
//
//        Expression iE = StringExpression.newBuilder().withValue(varName).build();
//        args.add(iE);
//        args.add(aeE);
//
//        Expression paren = ParenExpression.newBuilder().withParanExpr(aeE).build();
//
//        Expression iAE = AddressOperator.newBuilder().withOperatorExpression(paren).build();
//
//        args.add(CastOperatorExpression.newBuilder().
//                withCastType(uintptrType.clone()).withOperatorExpression(iAE).build());
//
//        args.addAll(aeE.getArrayIndices());
//
//        FunctionCallExpression fEx = FunctionCallExpression.newBuilder().
//                withFunctionName(functionName).
//                withArgs(VList.newInstance(args)).
//                build();
//
//        arrayAccessIt.set(fEx);
//    }
//
//    /**
//     * Adds a write-array instrumentation to the specified iterator.
//     *
//     * @param arrayAccessIt iterator to modify
//     * @param varAss array assignment statement to instrument
//     */
//    private static void addWriteArrayInstrumentation(VIterator arrayAccessIt, ArrayAssignmentStatement varAss) {
//        String varName = varAss.getVarName();
//
//        Type varType  = varAss.getDeclType();
//
//        if(varType==null) {
//            Optional<DeclStatement> varDeclStatement = varAss.parentScopes().get(0).
//                    resolveVariable(varName);
//            varType  = varDeclStatement.map(vD -> vD.getDeclType()).
//                    orElse(Type.newBuilder().withTypeName("void").build());
//        }
//
//        String functionName = "__vmf_w_" + varType.getTypeName() + "_d_"+varAss.getArrayIndices().size();
//
//        List<Expression> args = new ArrayList<>();
//
//        Expression label = StringExpression.newBuilder().withValue(computeLabel(varAss)).build();
//        args.add(label);
//
//        Expression iE = StringExpression.newBuilder().withValue("var:"+varName).build();
//        args.add(iE);
//
//        IdentifierExpression ie = IdentifierExpression.newBuilder().withVarName(varName).build();
//
//        ArrayAccessExpression acc = ArrayAccessExpression.newBuilder().
//                withArrayVariableExpression(ie).
//                withArrayIndices(varAss.getArrayIndices()).build();
//
//        args.add(acc);
//
//        Expression paren = ParenExpression.newBuilder().withParanExpr(acc).build();
//
//        Expression iAE = AddressOperator.newBuilder().withOperatorExpression(paren).build();
//
//        args.add(CastOperatorExpression.newBuilder().
//                withCastType(uintptrType.clone()).withOperatorExpression(iAE).build());
//
//        args.addAll(varAss.getArrayIndices());
//
//        FunctionCallStatement fEx = FunctionCallStatement.newBuilder().
//                withFunctionName(functionName).
//                withArgs(VList.newInstance(args)).
//                build();
//
//        arrayAccessIt.add(fEx);
//    }
//
//    private static String computeLabel(WithId expression) {
//
//        return "id=("+expression.getId()+"), " +
//                "location=("+ expression.getCodeRange().getStart().getIndex()
//                + ", " + expression.getCodeRange().getStop().getIndex() + ")";
//    }
//
//
//    /**
//     * Adds all necessary instrumentation methods for recording variable reads and writes.
//     *
//     * @param model model to modify
//     */
//    private static void addReadAndWriteVarInstrumentationFunctions(MiniClangModel model) {
//
//        // find max number of array dimensions by scanning all elements that
//        // declare arrays
//        int maxNumberOfArrayDimensions =
//                model.vmf().content().stream(WithArraySizes.class).
//                        map(was -> was.getArraySizes().size()).
//                        max(Integer::compareTo).orElse(0);
//
//        Program program = model.getRoot();
//
//
//        // var name param:
//        Parameter varNameP = Parameter.newBuilder().
//                withVarName("varName").withDeclType(stringType.clone()).build();
//
//        // label param:
//        Parameter labelP = Parameter.newBuilder().
//                withVarName("label").withDeclType(stringType.clone()).build();
//
//        // value types to instrument
//        List<Type> types = Arrays.asList(boolType, intType, doubleType);
//        List<Parameter> params = new ArrayList<>(types.size());
//        List<Parameter> addressParams = new ArrayList<>(types.size());
//
//        // create value params
//        for (Type t : types) {
//            Parameter varValueP = Parameter.newBuilder().
//                    withVarName("varValue").withDeclType(t.clone()).build();
//            params.add(varValueP);
//            Parameter varValueAddressP = Parameter.newBuilder().
//                    withVarName("varValueAddress").
//                    withDeclType(Type.newBuilder().
//                            withTypeName("uintptr_t ").build()).build();
//            addressParams.add(varValueAddressP);
//        }
//
//        // create forward declarations and implementations
//        for (int i = 0; i < params.size(); i++) {
//
//            Parameter p = params.get(i);
//
//            for (int dim = 0; dim <= maxNumberOfArrayDimensions; dim++) {
//                ForwardDecl fwDeclRead = ForwardDecl.newBuilder().
//                        withReturnType(p.getDeclType().clone()).
//                        withFunctionName("__vmf_r_" + p.getDeclType().getTypeName() + "_d_" + dim).build();
//
//                FunctionDecl fDeclRead = FunctionDecl.newBuilder().
//                        withReturnType(p.getDeclType().clone()).
//                        withFunctionName("__vmf_r_" + p.getDeclType().getTypeName() + "_d_" + dim).build();
//
//                ForwardDecl fwDeclWrite = ForwardDecl.newBuilder().
//                        withReturnType(Type.newBuilder().withTypeName("void").build()).
//                        withFunctionName("__vmf_w_" + p.getDeclType().getTypeName() + "_d_" + dim).build();
//
//                FunctionDecl fDeclWrite = FunctionDecl.newBuilder().
//                        withReturnType(Type.newBuilder().withTypeName("void").build()).
//                        withFunctionName("__vmf_w_" + p.getDeclType().getTypeName() + "_d_" + dim).build();
//
//                addStatementsAndParamsToInstrumentationFDecl(program, labelP, varNameP, params, addressParams, i, p, dim,
//                        fwDeclRead, fDeclRead, true);
//                addStatementsAndParamsToInstrumentationFDecl(program, labelP, varNameP, params, addressParams, i, p, dim,
//                        fwDeclWrite, fDeclWrite, false);
//            }
//        }
//    }
//
//    private static void addStatementsAndParamsToInstrumentationFDecl(Program program,
//                                                                     Parameter label,
//                                                                     Parameter varNameP,
//                                                                     List<Parameter> params,
//                                                                     List<Parameter> addressParams,
//                                                                     int i, Parameter p, int dim,
//                                                                     ForwardDecl fwDecl, FunctionDecl fDecl,
//                                                                     boolean read) {
//        if(read) {
//            fDecl.getStatements().add(newPrintStringStatement(" L "));
//        } else {
//            fDecl.getStatements().add(newPrintStringStatement(" S "));
//        }
//        fDecl.getStatements().add(newPrintIdentifierAddressStatement("varValueAddress"));
//        fDecl.getStatements().add(newPrintStringStatement(",0,"));
//        fDecl.getStatements().add(newPrintStringStatement("\\\"!"));
//        fDecl.getStatements().add(newPrintStringIdentifierStatement("varName"));
//        fDecl.getStatements().add(newPrintStringStatement(", value="));
//        addNewPrintIdentifierStatement(fDecl, "varValue", p);
//        fDecl.getStatements().add(newPrintStringStatement(", "));
//        fDecl.getStatements().add(newPrintStringIdentifierStatement("label"));
//
//        fwDecl.getParams().add(label.clone());
//        fwDecl.getParams().add(varNameP.clone());
//        fwDecl.getParams().add(params.get(i).clone());
//        fwDecl.getParams().add(addressParams.get(i).clone());
//
//        fDecl.getParams().add(label.clone());
//        fDecl.getParams().add(varNameP.clone());
//        fDecl.getParams().add(params.get(i).clone());
//        fDecl.getParams().add(addressParams.get(i).clone());
//
//        List<Parameter> indexPListRead = new ArrayList<>(dim);
//        for (int dimI = 0; dimI < dim; dimI++) {
//            Parameter indexParam = Parameter.newBuilder().
//                    withVarName("indexDim" + dimI).withDeclType(intType.clone()).build();
//            indexPListRead.add(indexParam);
//
//            fDecl.getStatements().add(newPrintStringStatement(", dim" + dimI + "="));
//            fDecl.getStatements().add(newPrintIntIdentifierStatement("indexDim" + dimI));
//        }
//
//        fDecl.getStatements().add(newPrintStringStatement("!\\\""));
//        fDecl.getStatements().add(newPrintStringStatement("\\n"));
//
//        if(read) {
//            fDecl.getStatements().add(ReturnStatement.newBuilder().
//                    withReturnValue(IdentifierExpression.newBuilder().
//                            withVarName("varValue").build()).build());
//        }
//
//        fDecl.getParams().addAll(indexPListRead);
//        fwDecl.getParams().addAll(indexPListRead);
//
//        program.getForwardDeclarations().add(fwDecl);
//        program.getFunctions().add(fDecl);
//    }
//
//    /**
//     * Adds a new print statement
//     *
//     * @param fDeclRead function declaration
//     * @param identifierName name of the variable
//     * @param p parameter for type information
//     */
//    private static void addNewPrintIdentifierStatement(FunctionDecl fDeclRead, String identifierName, Parameter p) {
//        if (p.getDeclType().equals(intType)) {
//            fDeclRead.getStatements().add(newPrintIntIdentifierStatement(identifierName));
//        } else if (p.getDeclType().equals(doubleType)) {
//            fDeclRead.getStatements().add(newPrintDoubleIdentifierStatement(identifierName));
//        } else if (p.getDeclType().equals(boolType)) {
//            fDeclRead.getStatements().add(newPrintBoolIdentifierStatement(identifierName));
//        } else if (p.getDeclType().equals(stringType)) {
//            fDeclRead.getStatements().add(newPrintStringIdentifierStatement(identifierName));
//        }
//    }
//
//    /**
//     * Returns a printf statement for string identifiers.
//     *
//     * @param varName variable name
//     * @return a printf statement for string identifiers
//     */
//    private static PrintStatement newPrintStringIdentifierStatement(String varName) {
//        StringExpression stringExpression = StringExpression.newBuilder().withValue("%s").build();
//        IdentifierExpression varNameExpr = IdentifierExpression.newBuilder().withVarName(varName).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(stringExpression).
//                withValueExpressions(VList.newInstance(Arrays.asList(varNameExpr))).build();
//    }
//
//    /**
//     * Returns a printf statement for int identifiers.
//     *
//     * @param varName variable name
//     * @return a printf statement for int identifiers
//     */
//    private static PrintStatement newPrintIntIdentifierStatement(String varName) {
//        StringExpression stringExpression = StringExpression.newBuilder().withValue("%i").build();
//        IdentifierExpression varNameExpr = IdentifierExpression.newBuilder().withVarName(varName).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(stringExpression).
//                withValueExpressions(VList.newInstance(Arrays.asList(varNameExpr))).build();
//    }
//
//    /**
//     * Returns a printf statement for double identifiers.
//     *
//     * @param varName variable name
//     * @return a printf statement for double identifiers
//     */
//    private static PrintStatement newPrintDoubleIdentifierStatement(String varName) {
//        StringExpression stringExpression = StringExpression.newBuilder().withValue("%lf").build();
//        IdentifierExpression varNameExpr = IdentifierExpression.newBuilder().withVarName(varName).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(stringExpression).
//                withValueExpressions(VList.newInstance(Arrays.asList(varNameExpr))).build();
//    }
//
//    /**
//     * Returns a printf statement for boolean identifiers.
//     *
//     * @param varName variable name
//     * @return a printf statement for boolean identifiers
//     */
//    private static PrintStatement newPrintBoolIdentifierStatement(String varName) {
//        StringExpression stringExpression = StringExpression.newBuilder().withValue("%i").build();
//        IdentifierExpression varNameExpr = IdentifierExpression.newBuilder().withVarName(varName).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(stringExpression).
//                withValueExpressions(VList.newInstance(Arrays.asList(varNameExpr))).build();
//    }
//
//    /**
//     * Returns a printf statement for value addresses.
//     *
//     * @param varName variable name
//     * @return a printf statement for value addresses
//     */
//    private static PrintStatement newPrintIdentifierAddressStatement(String varName) {
//        StringExpression stringExpression = StringExpression.newBuilder().withValue("%lx").build();
//        IdentifierExpression varNameExpr = IdentifierExpression.newBuilder().withVarName(varName).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(stringExpression).
//                withValueExpressions(VList.newInstance(Arrays.asList(varNameExpr))).build();
//    }
//
//    /**
//     * Returns a printf statement for string literals.
//     *
//     * @param s string to print
//     * @return a printf statement for string literals
//     */
//    private static PrintStatement newPrintStringStatement(String s) {
//        StringExpression strPrint1 = StringExpression.newBuilder().
//                withValue(s).build();
//        return PrintStatement.newBuilder().
//                withPrintExpression(strPrint1).build();
//    }
//
//    /**
//     * Instruments all block entries and exits of the specified model.
//     *
//     * @param fDecl function declaration to instrument
//     */
//    private static void instrumentBlockEntryAndExit(FunctionDecl fDecl) {
//        // add 'enter scope' & 'exit scope' instrumentation
//        fDecl.vmf().content().stream(ControlFlowScope.class).forEach(cF -> {
//            // create the 'enter scope' instrumentation
//            StringExpression stringExprEnter = StringExpression.newBuilder().
//                    withValue("== entering scope " + cF.getId() + "\\n").build();
//            PrintStatement pSEnter = PrintStatement.newBuilder().withPrintExpression(stringExprEnter).build();
//            // add it as first statement of this scope
//            cF.getStatements().add(0, pSEnter);
//
//            // create the 'exit scope' instrumentation
//            StringExpression stringExprExit = StringExpression.newBuilder().
//                    withValue("== exiting scope  " + cF.getId() + "\\n").build();
//            PrintStatement pSExit = PrintStatement.newBuilder().withPrintExpression(stringExprExit).build();
//
//            // for each return statement add additional 'exit scope' instrumentations calls since the
//            // return statement will skip every potential instrumentation at the end of the current scope
//            // hint: we do this in reverse order to preserve statement indices
//            for (int sI = cF.getStatements().size() - 1; sI > 0; sI--) {
//                if (cF.getStatements().get(sI) instanceof ReturnStatement) {
//                    cF.getStatements().add(sI, CommentStatement.newBuilder().
//                            withComment(PersistentComment.newBuilder().
//                                    withText(" -- begin return statement\n").build()).build());
//
//                    List<ControlFlowScope> ancestors = cF.parentScopes();
//
//                    // for each scope until we reach the method body scope
//                    for (ControlFlowScope cFP : ancestors) {
//                        // create the 'exit scope' instrumentation
//                        StringExpression stringExprExitInner = StringExpression.newBuilder().
//                                withValue("exiting scope  " + cFP.getId() + "\\n").build();
//                        PrintStatement pSExitInner = PrintStatement.newBuilder().
//                                withPrintExpression(stringExprExitInner).build();
//                        cF.getStatements().add(sI + 1, pSExitInner.clone());
//                    }
//
//                    // 'exit scope' instrumentations call for current scope
//                    cF.getStatements().add(sI + 1, pSExit.clone());
//
//                    cF.getStatements().add(sI + ancestors.size() + 3, CommentStatement.newBuilder().
//                            withComment(PersistentComment.newBuilder().
//                                    withText(" -- end   return statement\n").build()).build());
//                }
//            }
//
//            // add the 'exit scope' invocation as last statement of this scope
//            cF.getStatements().add(pSExit);
//        });
//    }
//
//
//    /**
//     * Converts all single statements that belong to loops or if-else statements to blocks.
//     *
//     * @param model model to transform
//     */
//    private static void convertStatementsToBlocks(MiniClangModel model) {
//        // converting loops & if-statements with just a single statement to block statements
//        model.vmf().content().stream(ControlFlowContainer.class).forEach(cfc -> {
//            if (cfc instanceof IfElseStatement) {
//                IfElseStatement ifElse = (IfElseStatement) cfc;
//                if (ifElse.getIfBlock() != null && !(ifElse.getIfBlock() instanceof BlockStatement)) {
//                    BlockStatement blockStatement = BlockStatement.newInstance();
//                    blockStatement.getStatements().add(ifElse.getIfBlock());
//                    ifElse.setIfBlock(blockStatement);
//                }
//                if (ifElse.getElseBlock() != null && !(ifElse.getElseBlock() instanceof BlockStatement)) {
//                    BlockStatement blockStatement = BlockStatement.newInstance();
//                    blockStatement.getStatements().add(ifElse.getElseBlock());
//                    ifElse.setElseBlock(blockStatement);
//                }
//            } else if (cfc instanceof ForStatement) {
//                ForStatement forLoop = (ForStatement) cfc;
//                if (forLoop.getBlock() != null && !(forLoop.getBlock() instanceof BlockStatement)) {
//                    BlockStatement blockStatement = BlockStatement.newInstance();
//                    blockStatement.getStatements().add(forLoop.getBlock());
//                    forLoop.setBlock(blockStatement);
//                }
//            } else if (cfc instanceof WhileStatement) {
//                WhileStatement whileLoop = (WhileStatement) cfc;
//                if (whileLoop.getBlock() != null && !(whileLoop.getBlock() instanceof BlockStatement)) {
//                    BlockStatement blockStatement = BlockStatement.newInstance();
//                    blockStatement.getStatements().add(whileLoop.getBlock());
//                    whileLoop.setBlock(blockStatement);
//                }
//            }
//        });
//    }
//}
//
//
///**
// * Simple formatter for C-style languages.
// */
//class MyFormatter extends BaseFormatter {
//
//    @Override
//    public void pre(MiniClangModelUnparser unparser, Formatter.RuleInfo ruleInfo, PrintWriter w) {
//
//        // TODO Allow rule consume() to provide manually...
//
//        String ruleText = ruleInfo.getRuleText();
//        String prevRuleText = getPrevRuleInfo().getRuleText();
//
//
//        if (Objects.equals(prevRuleText, "{")) {
//            incIndent();
//        }
//
//        if (Objects.equals(ruleText, "}")) {
//            decIndent();
//        }
//
//        boolean lineBreak = Objects.equals(prevRuleText, "{")
//                || Objects.equals(prevRuleText, "}")
//                || (Objects.equals(prevRuleText, ";")
//                || ruleText.equals("#define"));
//
//        if (matchInclude(prevRuleText)) {
//            lineBreak = true;
//        }
//
//        if (matchDefine(prevRuleText)) {
//            lineBreak = true;
//        }
//
//        if (insideFor(prevRuleText)) {
//            lineBreak = false;
//        }
//
//        if ("else".equals(ruleText) && "}".equals(prevRuleText)) {
//            lineBreak = false;
//            w.append(" ");
//        }
//
//
//        if (lineBreak) {
//            w.append('\n').append(getIndent());
//        } else if (IdentifierExpressionUnparser.
//                matchIdentifierExpressionAlt0(prevRuleText + ruleText)
//                && !ruleText.equals(";") && !ruleText.equals(")") && !ruleText.equals("(")
//                && !ruleText.equals(",")
//                || prevRuleText.equals(",")
//                || prevRuleText.equals("(")
//                || ruleText.equals(")")
//                || ruleText.equals("{")
//                || prevRuleText.equals("#define")
//                ) {
//            w.append(" ");
//        }
//
//        if (prevRuleText.startsWith("//") && prevRuleText.endsWith("\n")) {
//            w.append(getIndent());
//        }
//
//        super.pre(unparser, ruleInfo, w);
//
//    }
//
//    private boolean insideFor(String prevRuleText) {
//        if (prevRuleText.startsWith("for")) {
//            setBoolState("for-started", true);
//        }
//
//        if (getBoolState("for-started")) {
//            String stringState = getStringState("for-;");
//            if (stringState == null) stringState = "";
//            if (stringState.length() == 2) {
//                setStringState("for-;", "");
//                setBoolState("for-started", false);
//                return false;
//            }
//            if (";".equals(prevRuleText)) {
//                setStringState("for-;", stringState + ";");
//            }
//        }
//        return getBoolState("for-started");
//    }
//
//    private boolean matchInclude(String prevRuleText) {
//        boolean lineBreak = false;
//        if (prevRuleText.startsWith("#include")) {
//            setBoolState("include-started", true);
//        }
//        if (getBoolState("include-started")) {
//            setStringState("include-buffer", getStringState("include-buffer") + prevRuleText);
//            if (">".equals(prevRuleText)) {
//                setBoolState("include-started", false);
//                setStringState("include-buffer", "");
//                lineBreak = true;
//            }
//        }
//        return lineBreak;
//    }
//
//    private boolean matchDefine(String prevRuleText) {
//        boolean lineBreak = false;
//        if (prevRuleText.startsWith("#define")) {
//            setBoolState("define-started", true);
//        }
//        if (getBoolState("define-started")) {
//            setStringState("define-buffer", getStringState("define-buffer") + prevRuleText);
//            if (IntLiteralUnparser.matchIntLiteralAlt0(prevRuleText)) {
//                setBoolState("define-started", false);
//                setStringState("define-buffer", "");
//                lineBreak = true;
//            }
//        }
//        return lineBreak;
//    }
//
//}
//
////        // append '_123' to all variable names and make sure the program still runs
////        model.vmf().content().stream().filter(e->e instanceof WithVarName).
////                map(e -> (WithVarName)e).forEach(e->e.setVarName(e.getVarName()+"_123"));
////        model.vmf().content().stream().filter(e->e instanceof WithArraySizes).
////                map(e->(WithArraySizes)e).forEach(p-> {
////            for(int i = 0; i < p.getArraySizes().size();i++) {
////                String s = p.getArraySizes().get(i);
////                if(IdentifierExpressionUnparser.matchIdentifierExpressionAlt0(s)) {
////                    p.getArraySizes().set(i,s+"_123");
////                }
////            }
////        });
//
//
//
