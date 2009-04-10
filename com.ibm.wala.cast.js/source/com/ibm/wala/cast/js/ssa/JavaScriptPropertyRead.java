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
package com.ibm.wala.cast.js.ssa;

import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class JavaScriptPropertyRead extends AbstractReflectiveGet {
  public JavaScriptPropertyRead(int result, int objectRef, int memberRef) {
    super(result, objectRef, memberRef);
  }

  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return
      ((JSInstructionFactory)insts).PropertyRead(
        defs==null? getDef(): defs[0],
	uses==null? getObjectRef(): uses[0],
	uses==null? getMemberRef(): uses[1]);
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#isPEI()
   */
  public boolean isPEI() {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

  /**
  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.SSAInstruction#visit(com.ibm.domo.ssa.SSAInstruction.Visitor)
   */
  public void visit(IVisitor v) {
    Assertions._assert(v instanceof InstructionVisitor);
    ((InstructionVisitor)v).visitJavaScriptPropertyRead(this);
  }
}
