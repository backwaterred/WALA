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
/*
 * Created on Sep 27, 2005
 */
package com.ibm.wala.cast.java.ipa.callgraph;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.strings.Atom;

public class JavaSourceAnalysisScope extends AnalysisScope {

  public final static ClassLoaderReference SOURCE = new ClassLoaderReference(Atom.findOrCreateAsciiAtom("Source"), Atom
      .findOrCreateAsciiAtom("Java"), ClassLoaderReference.Application);
 
  public JavaSourceAnalysisScope() {
    this(Collections.singleton(Language.JAVA));
  }

  protected void initCoreForJavaSource() {
    initCoreForJava();
    loadersByName.put(SOURCE.getName(),SOURCE);

    setLoaderImpl(SOURCE, "com.ibm.wala.cast.java.translator.polyglot.PolyglotSourceLoaderImpl");   
  }
  
  protected JavaSourceAnalysisScope(Collection<? extends Language> languages) {
    super(languages);
    initCoreForJavaSource();
    initSynthetic(SOURCE);
   }

  public ClassLoaderReference getSourceLoader() {
    return SOURCE;
  }
}
