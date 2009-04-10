package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.FieldReference;

public interface AstInstructionFactory extends SSAInstructionFactory {

  AssignInstruction AssignInstruction(int result, int val);
  
  AstAssertInstruction AssertInstruction(int value, boolean fromSpecification);
  
  AstEchoInstruction EchoInstruction(int[] rvals);
  
  AstGlobalRead GlobalRead(int lhs, FieldReference global);
  
  AstGlobalWrite GlobalWrite(FieldReference global, int rhs);
  
  AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal, FieldReference fieldRef);
  
  AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, FieldReference fieldRef);
  
  AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval, int fieldVal);
  
  AstIsDefinedInstruction IsDefinedInstruction(int lval, int rval);
  
  AstLexicalRead LexicalRead(Access[] accesses);
  
  AstLexicalRead LexicalRead(Access access);
  
  AstLexicalRead LexicalRead(int lhs, String definer, String globalName);
  
  AstLexicalWrite LexicalWrite(Access[] accesses);
  
  AstLexicalWrite LexicalWrite(Access access);
  
  AstLexicalWrite LexicalWrite(String definer, String globalName, int rhs);
  
  EachElementGetInstruction EachElementGetInstruction(int lValue, int objectRef);
  
  EachElementHasNextInstruction EachElementHasNextInstruction(int lValue, int objectRef);
  
}
