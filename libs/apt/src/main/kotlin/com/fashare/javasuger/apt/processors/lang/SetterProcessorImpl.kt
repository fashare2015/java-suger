package com.fashare.javasuger.apt.processors.lang

import com.fashare.javasuger.annotation.lang.Setter
import com.fashare.javasuger.apt.base.SingleAnnotationProcessor
import com.sun.source.tree.Tree
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeTranslator
import com.sun.tools.javac.util.List
import com.sun.tools.javac.util.ListBuffer
import com.sun.tools.javac.util.Name
import javax.lang.model.element.TypeElement

internal class SetterProcessorImpl : SingleAnnotationProcessor() {
    override val mAnnotation = Setter::class.java

    override fun translator(curElement: TypeElement, curTree: JCTree, rootTree: JCCompilationUnit) {
        curTree.accept(MyTreeTranslator(curElement.simpleName as Name))
    }

    inner class MyTreeTranslator(val rootClazzName: Name) : TreeTranslator() {
        val shouldReturnThis = true

        override fun visitClassDef(jcClassDecl: JCClassDecl) {
            if (jcClassDecl.name.equals(rootClazzName)) {
                treeMaker.at(jcClassDecl.pos)
                jcClassDecl.defs
                        .filter { it.kind == Tree.Kind.VARIABLE }
                        .map { it as JCVariableDecl }
                        .forEach {
                            jcClassDecl.defs = jcClassDecl.defs.append(makeSetterMethodDecl(it, jcClassDecl))
                        }
            }
            super.visitClassDef(jcClassDecl)
        }

        private fun makeSetterMethodDecl(jcVariableDecl: JCVariableDecl, jcClassDecl: JCClassDecl): JCTree {
            val body = ListBuffer<JCStatement>()
                    .append(treeMaker.Exec(treeMaker.Assign(
                            treeMaker.Select(treeMaker.Ident(names._this), jcVariableDecl.getName()),
                            treeMaker.Ident(jcVariableDecl.name)
                    )))
                    .apply {
                        if (shouldReturnThis) {
                            this.append(treeMaker.Return(treeMaker.Ident(names._this)))
                        }
                    }
                    .toList()
                    .let { treeMaker.Block(0, it) }

            return treeMaker.MethodDef(
                    treeMaker.Modifiers(Flags.PUBLIC.toLong()),
                    getNewMethodName(jcVariableDecl.getName()),
                    if (shouldReturnThis) {
                        treeMaker.Ident(jcClassDecl.name)
                    } else {
                        treeMaker.TypeIdent(TypeTag.VOID)
                    },
                    List.nil(),
                    List.of(treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER),
                            jcVariableDecl.name,
                            jcVariableDecl.vartype,
                            null)
                    ),
                    List.nil(),
                    body, null)
        }

        private fun getNewMethodName(name: Name): Name {
            val str = name.toString()
            if (str.isNotEmpty()) {
                return names.fromString("set" + str.substring(0, 1).toUpperCase() + str.substring(1, str.length))
            } else {
                return names.fromString("set")
            }
        }
    }
}