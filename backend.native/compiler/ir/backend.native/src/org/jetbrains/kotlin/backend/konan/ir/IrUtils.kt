/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.io.StringWriter


fun ir2string(ir: IrElement?): String = ir2stringWhole(ir).takeWhile { it != '\n' }

fun ir2stringWhole(ir: IrElement?, withDescriptors: Boolean = false): String {
    val strWriter = StringWriter()

    if (withDescriptors)
        ir?.accept(DumpIrTreeWithDescriptorsVisitor(strWriter), "")
    else
        ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

internal fun ClassDescriptor.createSimpleDelegatingConstructorDescriptor(superConstructorDescriptor: ClassConstructorDescriptor, isPrimary: Boolean = false)
        : ClassConstructorDescriptor {
    val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            /* containingDeclaration = */ this,
            /* annotations           = */ Annotations.EMPTY,
            /* isPrimary             = */ isPrimary,
            /* source                = */ SourceElement.NO_SOURCE)
    val valueParameters = superConstructorDescriptor.valueParameters.map {
        it.copy(constructorDescriptor, it.name, it.index)
    }
    constructorDescriptor.initialize(valueParameters, superConstructorDescriptor.visibility)
    constructorDescriptor.returnType = superConstructorDescriptor.returnType
    return constructorDescriptor
}

internal fun ClassDescriptor.createSimpleDelegatingConstructor(superConstructorDescriptor: ClassConstructorDescriptor,
                                                               constructorDescriptor: ClassConstructorDescriptor,
                                                               startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin)
        : IrConstructor {
    val body = IrBlockBodyImpl(startOffset, endOffset,
            listOf(
                    IrDelegatingConstructorCallImpl(startOffset, endOffset, superConstructorDescriptor).apply {
                        constructorDescriptor.valueParameters.forEachIndexed { idx, parameter ->
                            putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter))
                        }
                    },
                    IrInstanceInitializerCallImpl(startOffset, endOffset, this)
            )
    )
    return IrConstructorImpl(startOffset, endOffset, origin, constructorDescriptor, body)
}

internal fun Context.createArrayOfExpression(arrayElementType: KotlinType,
                                             arrayElements: List<IrExpression>,
                                             startOffset: Int, endOffset: Int): IrExpression {
    val kotlinPackage = irModule!!.descriptor.getPackage(FqName("kotlin"))
    val genericArrayOfFun = kotlinPackage.memberScope.getContributedFunctions(Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND).first()
    val typeParameter0 = genericArrayOfFun.typeParameters[0]
    val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
    val substitutedArrayOfFun = genericArrayOfFun.substitute(typeSubstitutor)!!

    val typeArguments = mapOf(typeParameter0 to arrayElementType)

    val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
    val arg0VarargType = valueParameter0.type
    val arg0VarargElementType = valueParameter0.varargElementType!!
    val arg0 = IrVarargImpl(startOffset, endOffset, arg0VarargType, arg0VarargElementType, arrayElements)

    return IrCallImpl(startOffset, endOffset, substitutedArrayOfFun, typeArguments).apply {
        putValueArgument(0, arg0)
    }
}
