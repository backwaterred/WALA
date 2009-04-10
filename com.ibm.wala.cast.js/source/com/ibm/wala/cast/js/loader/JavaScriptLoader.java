/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.loader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.js.ssa.*;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstDynamicPropertyClass;
import com.ibm.wala.cast.loader.AstFunctionClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.LanguageImpl;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IComparisonInstruction.Operator;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class JavaScriptLoader extends CAstAbstractLoader {

  public final static Language JS = new LanguageImpl() {

    public Atom getName() {
      return Atom.findOrCreateUnicodeAtom("JavaScript");
    }

    public TypeReference getRootType() {
      return JavaScriptTypes.Root;
    }

    public TypeReference getThrowableType() {
      return JavaScriptTypes.Root;
    }

    public TypeReference getConstantType(Object o) {
      if (o == null) {
	return JavaScriptTypes.Null;
      } else {
	Class c = o.getClass();
	if (c == Boolean.class) {
	  return JavaScriptTypes.Boolean; 
	} else if (c == String.class) {
	  return JavaScriptTypes.String; 
	} else if (c == Integer.class) { 
	  return JavaScriptTypes.Number; 
	} else if (c == Float.class) {
	  return JavaScriptTypes.Number; 
	} else if (c == Double.class) {
	  return JavaScriptTypes.Number; 
	} else {
	  assert false : "cannot determine type for " + o + " of class " + c;
	  return null;
	}
      }
    }

    public boolean isNullType(TypeReference type) {
      return 
	type.equals(JavaScriptTypes.Undefined)
	                 || 
	type.equals(JavaScriptTypes.Null);
    }

    public TypeReference[] getArrayInterfaces() {
       return new TypeReference[0];
    }

    public TypeName lookupPrimitiveType(String name) {
      if ("Boolean".equals(name)) {
        return JavaScriptTypes.Boolean.getName();
      } else if ("Number".equals(name)) {
        return JavaScriptTypes.Number.getName();
      } else if ("String".equals(name)) {
        return JavaScriptTypes.String.getName();
      } else if ("Date".equals(name)) {
        return JavaScriptTypes.Date.getName();
      } else {
        assert "RegExp".equals(name);
        return JavaScriptTypes.RegExp.getName();
      }
    }

    public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
        throws InvalidClassFileException {
      return Collections.singleton(JavaScriptTypes.Root);
    }

    public Object getMetadataToken(Object value) {
      assert false;
      return null;
    }

    public JSInstructionFactory instructionFactory() {
      return new JSInstructionFactory() {

        public JavaScriptCheckReference CheckReference(int ref) {
          return new JavaScriptCheckReference(ref);
        }

        public SSAGetInstruction GetInstruction(int result, int ref, String field) {
          return GetInstruction(result, ref, FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(field), JavaScriptTypes.Root));
        }

        public JavaScriptInstanceOf InstanceOf(int result, int objVal, int typeVal) {
          return new JavaScriptInstanceOf(result, objVal, typeVal);
        }

        public JavaScriptInvoke Invoke(int function, int[] results, int[] params, int exception, CallSiteReference site) {
           return new JavaScriptInvoke(function, results, params, exception, site);
        }

        public JavaScriptInvoke Invoke(int function, int[] results, int[] params, int exception, CallSiteReference site,
            Access[] lexicalReads, Access[] lexicalWrites) {
          return new JavaScriptInvoke(function, results, params, exception, site, lexicalReads, lexicalWrites);
        }

        public JavaScriptInvoke Invoke(int function, int result, int[] params, int exception, CallSiteReference site) {
          return new JavaScriptInvoke(function, result, params, exception, site);
        }

        public JavaScriptInvoke Invoke(int function, int[] params, int exception, CallSiteReference site) {
           return new JavaScriptInvoke(function, params, exception, site);
        }

        public JavaScriptPropertyRead PropertyRead(int result, int objectRef, int memberRef) {
           return new JavaScriptPropertyRead(result, objectRef, memberRef);
        }

        public JavaScriptPropertyWrite PropertyWrite(int objectRef, int memberRef, int value) {
          return new JavaScriptPropertyWrite(objectRef, memberRef, value);
        }

        public SSAPutInstruction PutInstruction(int ref, int value, String field) {
          return PutInstruction(ref, value, FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(field), JavaScriptTypes.Root));
        }

        public JavaScriptTypeOfInstruction TypeOfInstruction(int lval, int object) {
           return new JavaScriptTypeOfInstruction(lval, object);
        }

        public JavaScriptWithRegion WithRegion(int expr, boolean isEnter) {
          return new JavaScriptWithRegion(expr, isEnter);
        }

        public AstAssertInstruction AssertInstruction(int value, boolean fromSpecification) {
          return new AstAssertInstruction(value, fromSpecification);
        }

        public com.ibm.wala.cast.ir.ssa.AssignInstruction AssignInstruction(int result, int val) {
          return new AssignInstruction(result, val);
        }

        public com.ibm.wala.cast.ir.ssa.EachElementGetInstruction EachElementGetInstruction(int value, int objectRef) {
          return new EachElementGetInstruction(value, objectRef);
        }

        public com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction EachElementHasNextInstruction(int value, int objectRef) {
           return new EachElementHasNextInstruction(value, objectRef);
        }

        public AstEchoInstruction EchoInstruction(int[] rvals) {
          return new AstEchoInstruction(rvals);
        }

        public AstGlobalRead GlobalRead(int lhs, FieldReference global) {
          return new AstGlobalRead(lhs, global);
        }

        public AstGlobalWrite GlobalWrite(FieldReference global, int rhs) {
          return new AstGlobalWrite(global, rhs);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal, FieldReference fieldRef) {
           return new AstIsDefinedInstruction(lval, rval, fieldVal, fieldRef);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, FieldReference fieldRef) {
          return new AstIsDefinedInstruction(lval, rval, fieldRef);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal) {
           return new AstIsDefinedInstruction(lval, rval, fieldVal);
        }

        public AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval) {
          return new AstIsDefinedInstruction(lval, rval);
        }

        public AstLexicalRead LexicalRead(Access[] accesses) {
          return new AstLexicalRead(accesses);
        }

        public AstLexicalRead LexicalRead(Access access) {
           return new AstLexicalRead(access);
        }

        public AstLexicalRead LexicalRead(int lhs, String definer, String globalName) {
          return new AstLexicalRead(lhs, definer, globalName);
        }

        public AstLexicalWrite LexicalWrite(Access[] accesses) {
          return new AstLexicalWrite(accesses);
        }

        public AstLexicalWrite LexicalWrite(Access access) {
          return new AstLexicalWrite(access);
        }

        public AstLexicalWrite LexicalWrite(String definer, String globalName, int rhs) {
           return new AstLexicalWrite(definer, globalName, rhs);
        }

        public SSAArrayLengthInstruction ArrayLengthInstruction(int result, int arrayref) {
          throw new UnsupportedOperationException();
        }

        public SSAArrayLoadInstruction ArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        public SSAArrayStoreInstruction ArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType) {
          throw new UnsupportedOperationException();
        }

        public SSABinaryOpInstruction BinaryOpInstruction(IOperator operator, boolean overflow, boolean unsigned, int result,
            int val1, int val2, boolean mayBeInteger) {
          return new SSABinaryOpInstruction(operator, result, val1, val2, mayBeInteger) {
            public boolean isPEI() {
              return false;
            }
            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
              return insts.BinaryOpInstruction(getOperator(), false, false, defs == null || defs.length == 0 ? getDef(0) : defs[0], uses == null ? getUse(0) : uses[0],
                 uses == null ? getUse(1) : uses[1], mayBeIntegerOp());
           }
          };
        }

        public SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference type) {
          throw new UnsupportedOperationException();
        }

        public SSAComparisonInstruction ComparisonInstruction(Operator operator, int result, int val1, int val2) {
           return new SSAComparisonInstruction(operator, result, val1, val2);
        }

        public SSAConditionalBranchInstruction ConditionalBranchInstruction(
            com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2) {
           return new SSAConditionalBranchInstruction(operator, type, val1, val2);
        }

        public SSAConversionInstruction ConversionInstruction(int result, int val, TypeReference fromType, TypeReference toType,
            boolean overflow) {
          assert !overflow;
          return new SSAConversionInstruction(result, val, fromType, toType) {
            @Override
            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) throws IllegalArgumentException {
              if (uses != null && uses.length == 0) {
                throw new IllegalArgumentException("(uses != null) and (uses.length == 0)");
              }
              return insts.ConversionInstruction(defs == null || defs.length == 0 ? getDef(0) : defs[0], uses == null ? getUse(0) : uses[0],
                  getFromType(), getToType(), false);
            }
          };
        }

        public SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
          return new SSAGetCaughtExceptionInstruction(bbNumber, exceptionValueNumber);
        }

        public SSAGetInstruction GetInstruction(int result, FieldReference field) {
          throw new UnsupportedOperationException();
       }

        public SSAGetInstruction GetInstruction(int result, int ref, FieldReference field) {
          return new SSAGetInstruction(result, ref, field) {
            public boolean isPEI() {
              return false;
            }
          };
        }

        public SSAGotoInstruction GotoInstruction() {
           return new SSAGotoInstruction();
        }

        public SSAInstanceofInstruction InstanceofInstruction(int result, int ref, TypeReference checkedType) {
          throw new UnsupportedOperationException();
        }

        public SSAInvokeInstruction InvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
        }

        public SSAInvokeInstruction InvokeInstruction(int[] params, int exception, CallSiteReference site) {
          throw new UnsupportedOperationException();
       }

        public SSALoadMetadataInstruction LoadMetadataInstruction(int lval, TypeReference entityType, Object token) {
          throw new UnsupportedOperationException();
        }

        public SSAMonitorInstruction MonitorInstruction(int ref, boolean isEnter) {
          throw new UnsupportedOperationException();
        }

        public SSANewInstruction NewInstruction(int result, NewSiteReference site) {
          return new SSANewInstruction(result, site) {
            public boolean isPEI() {
              return true;
            }
            public Collection<TypeReference> getExceptionTypes() {
              return Collections.singleton(JavaScriptTypes.TypeError);
            }
          };
        }

        public SSANewInstruction NewInstruction(int result, NewSiteReference site, int[] params) {
          throw new UnsupportedOperationException();
        }

        public SSAPhiInstruction PhiInstruction(int result, int[] params) {
           return new SSAPhiInstruction(result, params);
        }

        public SSAPiInstruction PiInstruction(int result, int val, int piBlock, int successorBlock, SSAInstruction cause) {
          return new SSAPiInstruction(result, val, piBlock, successorBlock, cause);
        }

        public SSAPutInstruction PutInstruction(int ref, int value, FieldReference field) {
          return new SSAPutInstruction(ref, value, field) {
            public boolean isPEI() {
              return false;
            }
          };
        }

        public SSAPutInstruction PutInstruction(int value, FieldReference field) {
          throw new UnsupportedOperationException();
       }

        public SSAReturnInstruction ReturnInstruction() {
          return new SSAReturnInstruction();
        }

        public SSAReturnInstruction ReturnInstruction(int result, boolean isPrimitive) {
          return new SSAReturnInstruction(result, isPrimitive);
        }

        public SSASwitchInstruction SwitchInstruction(int val, int defaultLabel, int[] casesAndLabels) {
          return new SSASwitchInstruction(val, defaultLabel, casesAndLabels);
        }

        public SSAThrowInstruction ThrowInstruction(int exception) {
          return new SSAThrowInstruction(exception) {
            public boolean isPEI() {
              return true;
            }
            public Collection<TypeReference> getExceptionTypes() {
              return Collections.emptySet();
            }
          };
        }

        public SSAUnaryOpInstruction UnaryOpInstruction(com.ibm.wala.shrikeBT.IUnaryOpInstruction.IOperator operator, int result,
            int val) {
           return new SSAUnaryOpInstruction(operator, result, val);
        }
        
      };
    }

    public boolean isDoubleType(TypeReference type) {
      return type == JavaScriptTypes.Number || type == JavaScriptTypes.NumberObject;
    }

    public boolean isFloatType(TypeReference type) {
      return false;
    }

    public boolean isIntType(TypeReference type) {
      return false;
    }

    public boolean isLongType(TypeReference type) {
       return false;
    }

    public boolean isMetadataType(TypeReference type) {
       return false;
    }

    public boolean isStringType(TypeReference type) {
       return type == JavaScriptTypes.String || type == JavaScriptTypes.StringObject;
    }
  };

  private static final Map<Selector,IMethod> emptyMap1 = Collections.emptyMap();
  private static final Map<Atom,IField> emptyMap2 = Collections.emptyMap();

  private final JavaScriptTranslatorFactory translatorFactory;

  public JavaScriptLoader(IClassHierarchy cha, JavaScriptTranslatorFactory translatorFactory) {
    super(cha);
    this.translatorFactory = translatorFactory;
  }

  class JavaScriptClass extends AstClass {
    private IClass superClass;

    private JavaScriptClass(IClassLoader loader, TypeReference classRef, TypeReference superRef,
        CAstSourcePositionMap.Position sourcePosition) {
      super(sourcePosition, classRef.getName(), loader, (short) 0, emptyMap2, emptyMap1);
      types.put(classRef.getName(), this);
      superClass = superRef == null ? null : loader.lookupClass(superRef.getName());
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "JS:" + getReference().toString();
    }

    @Override
    public Collection<IClass> getDirectInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    @Override
    public IClass getSuperclass() throws ClassHierarchyException {
      return superClass;
    }
  }

  class JavaScriptRootClass extends AstDynamicPropertyClass {

    private JavaScriptRootClass(IClassLoader loader, CAstSourcePositionMap.Position sourcePosition) {
      super(sourcePosition, JavaScriptTypes.Root.getName(), loader, (short) 0, emptyMap1, JavaScriptTypes.Root);

      types.put(JavaScriptTypes.Root.getName(), this);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "JS Root:" + getReference().toString();
    }

    public Collection<IClass> getDirectInterfaces() throws ClassHierarchyException {
      return Collections.emptySet();
    }

    public IClass getSuperclass() throws ClassHierarchyException {
      return null;
    }
  }

  class JavaScriptCodeBody extends AstFunctionClass {

    public JavaScriptCodeBody(TypeReference codeName, TypeReference parent, IClassLoader loader,
        CAstSourcePositionMap.Position sourcePosition) {
      super(codeName, parent, loader, sourcePosition);
      types.put(codeName.getName(), this);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    private IMethod setCodeBody(IMethod codeBody) {
      this.functionBody = codeBody;
      return codeBody;
    }
  }

  private final Set<CAstQualifier> functionQualifiers;

  {
    functionQualifiers = HashSetFactory.make();
    functionQualifiers.add(CAstQualifier.PUBLIC);
    functionQualifiers.add(CAstQualifier.FINAL);
  }

  public class JavaScriptMethodObject extends AstMethod {

    JavaScriptMethodObject(JavaScriptCodeBody cls, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
        TypeReference[][] caughtTypes, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(cls, functionQualifiers, cfg, symtab, AstMethodReference.fnReference(cls.getReference()), hasCatchBlock, caughtTypes,
          lexicalInfo, debugInfo);
    }

    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    public String toString() {
      return "<Code body of " + cls + ">";
    }

    public TypeReference[] getDeclaredExceptions() {
      return null;
    }

    public LexicalParent[] getParents() {
      if (lexicalInfo() == null)
        return new LexicalParent[0];

      final String[] parents = lexicalInfo().getScopingParents();

      if (parents == null)
        return new LexicalParent[0];

      LexicalParent result[] = new LexicalParent[parents.length];

      for (int i = 0; i < parents.length; i++) {
        final int hack = i;
        final AstMethod method = (AstMethod) lookupClass(parents[i], cha).getMethod(AstMethodReference.fnSelector);
        result[i] = new LexicalParent() {
          public String getName() {
            return parents[hack];
          }

          public AstMethod getMethod() {
            return method;
          }
        };

        System.err.println(("parent " + result[i].getName() + " is " + result[i].getMethod()));
      }

      return result;
    }

    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

    public boolean hasLocalVariableTable() {
      return false;
    }

    public int getMaxLocals() {
      Assertions.UNREACHABLE();
      return -1;
    }

    public int getMaxStackHeight() {
      Assertions.UNREACHABLE();
      return -1;
    }

    public TypeReference getParameterType(int i) {
      return JavaScriptTypes.Root;
    }
  }

  public IClass defineCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition) {
    return new JavaScriptCodeBody(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName(name)), P, this,
        sourcePosition);
  }

  public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos) {
    return defineCodeBodyType(name, JavaScriptTypes.Function, pos);
  }

  public IClass defineScriptType(String name, CAstSourcePositionMap.Position pos) {
    return defineCodeBodyType(name, JavaScriptTypes.Script, pos);
  }

  public IMethod defineCodeBodyCode(String clsName, AbstractCFG cfg, SymbolTable symtab, boolean hasCatchBlock,
      TypeReference[][] caughtTypes, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
    JavaScriptCodeBody C = (JavaScriptCodeBody) lookupClass(clsName, cha);
    Assertions._assert(C != null, clsName);
    return C.setCodeBody(new JavaScriptMethodObject(C, cfg, symtab, hasCatchBlock, caughtTypes, lexicalInfo, debugInfo));
  }

  final JavaScriptRootClass ROOT = new JavaScriptRootClass(this, null);

  final JavaScriptClass UNDEFINED = new JavaScriptClass(this, JavaScriptTypes.Undefined, JavaScriptTypes.Root, null);

  final JavaScriptClass PRIMITIVES = new JavaScriptClass(this, JavaScriptTypes.Primitives, JavaScriptTypes.Root, null);

  final JavaScriptClass FAKEROOT = new JavaScriptClass(this, JavaScriptTypes.FakeRoot, JavaScriptTypes.Root, null);

  final JavaScriptClass STRING = new JavaScriptClass(this, JavaScriptTypes.String, JavaScriptTypes.Root, null);

  final JavaScriptClass NULL = new JavaScriptClass(this, JavaScriptTypes.Null, JavaScriptTypes.Root, null);

  final JavaScriptClass ARRAY = new JavaScriptClass(this, JavaScriptTypes.Array, JavaScriptTypes.Root, null);

  final JavaScriptClass OBJECT = new JavaScriptClass(this, JavaScriptTypes.Object, JavaScriptTypes.Root, null);

  final JavaScriptClass TYPE_ERROR = new JavaScriptClass(this, JavaScriptTypes.TypeError, JavaScriptTypes.Root, null);

  final JavaScriptClass CODE_BODY = new JavaScriptClass(this, JavaScriptTypes.CodeBody, JavaScriptTypes.Root, null);

  final JavaScriptClass FUNCTION = new JavaScriptClass(this, JavaScriptTypes.Function, JavaScriptTypes.CodeBody, null);

  final JavaScriptClass SCRIPT = new JavaScriptClass(this, JavaScriptTypes.Script, JavaScriptTypes.CodeBody, null);

  final JavaScriptClass BOOLEAN = new JavaScriptClass(this, JavaScriptTypes.Boolean, JavaScriptTypes.Root, null);

  final JavaScriptClass NUMBER = new JavaScriptClass(this, JavaScriptTypes.Number, JavaScriptTypes.Root, null);

  final JavaScriptClass DATE = new JavaScriptClass(this, JavaScriptTypes.Date, JavaScriptTypes.Root, null);

  final JavaScriptClass REGEXP = new JavaScriptClass(this, JavaScriptTypes.RegExp, JavaScriptTypes.Root, null);

  final JavaScriptClass BOOLEAN_OBJECT = new JavaScriptClass(this, JavaScriptTypes.BooleanObject, JavaScriptTypes.Object, null);

  final JavaScriptClass NUMBER_OBJECT = new JavaScriptClass(this, JavaScriptTypes.NumberObject, JavaScriptTypes.Object, null);

  final JavaScriptClass DATE_OBJECT = new JavaScriptClass(this, JavaScriptTypes.DateObject, JavaScriptTypes.Object, null);

  final JavaScriptClass REGEXP_OBJECT = new JavaScriptClass(this, JavaScriptTypes.RegExpObject, JavaScriptTypes.Object, null);

  final JavaScriptClass STRING_OBJECT = new JavaScriptClass(this, JavaScriptTypes.StringObject, JavaScriptTypes.Object, null);

  public Language getLanguage() {
    return JS;
  }

  public ClassLoaderReference getReference() {
    return JavaScriptTypes.jsLoader;
  }

  public SSAInstructionFactory getInstructionFactory() {
     return JS.instructionFactory();
  }

  public void init(List<Module> modules) throws IOException {
    translatorFactory.make(this).translate(modules);
  }

}
