/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 * 
 */
public class SSAGetCaughtExceptionInstruction extends SSAInstruction {
  private final int exceptionValueNumber;

  private final int bbNumber;

  public SSAGetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
    super();
    this.exceptionValueNumber = exceptionValueNumber;
    this.bbNumber = bbNumber;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    assert defs == null || defs.length == 1;
    return insts.GetCaughtExceptionInstruction(bbNumber, defs == null ? exceptionValueNumber : defs[0]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    StringBuffer s = new StringBuffer();
    s.append(getValueString(symbolTable, exceptionValueNumber)).append(" = getCaughtException ");
    return s.toString();
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException
   *             if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitGetCaughtException(this);
  }

  /**
   * Returns the result.
   * 
   * @return int
   */
  public int getException() {
    return exceptionValueNumber;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return exceptionValueNumber;
  }

  @Override
  public int getDef(int i) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
    return exceptionValueNumber;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * Returns the bb.
   * 
   * @return BasicBlock
   */
  public int getBasicBlockNumber() {
    return bbNumber;
  }

  @Override
  public int hashCode() {
    return 2243 * exceptionValueNumber;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

}
