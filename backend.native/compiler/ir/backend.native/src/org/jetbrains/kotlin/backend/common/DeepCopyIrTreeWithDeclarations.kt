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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree

/**
 * Copies IR tree with descriptors of all declarations inside;
 * updates the references to these declarations.
 */
open class DeepCopyIrTreeWithDeclarations : DeepCopyIrTree() {

    private fun DeclarationDescriptor.notSupported(): Nothing = TODO("${this}")

    override fun mapModuleDescriptor(descriptor: ModuleDescriptor) = descriptor.notSupported()
    override fun mapPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor) = descriptor.notSupported()
    override fun mapClassDeclaration(descriptor: ClassDescriptor) = descriptor.notSupported()
    override fun mapTypeAliasDeclaration(descriptor: TypeAliasDescriptor) = descriptor.notSupported()
    override fun mapFunctionDeclaration(descriptor: FunctionDescriptor) = descriptor.notSupported()
    override fun mapConstructorDeclaration(descriptor: ClassConstructorDescriptor) = descriptor.notSupported()
    override fun mapPropertyDeclaration(descriptor: PropertyDescriptor) = descriptor.notSupported()
    override fun mapLocalPropertyDeclaration(descriptor: VariableDescriptorWithAccessors) = descriptor.notSupported()
    override fun mapEnumEntryDeclaration(descriptor: ClassDescriptor) = descriptor.notSupported()

    val copiedVariables = mutableMapOf<VariableDescriptor, VariableDescriptor>()

    override fun mapVariableDeclaration(descriptor: VariableDescriptor): VariableDescriptor {
        // TODO: how to ensure that the variable is not visible from outside of the transformed IR?

        if (descriptor is VariableDescriptorWithAccessors && (descriptor.getter != null || descriptor.setter != null)) {
            TODO("$descriptor with accessors")
        }

        val newDescriptor = LocalVariableDescriptor(
                /* containingDeclaration = */ descriptor.containingDeclaration,
                /* annotations = */ descriptor.annotations,
                /* name = */ descriptor.name,
                /* type = */ descriptor.type,
                /* mutable = */ descriptor.isVar,
                /* isDelegated = */ false,
                /* source = */ descriptor.source
        )

        assert (descriptor !in copiedVariables)
        copiedVariables[descriptor] = newDescriptor

        return newDescriptor
    }

    // Note: the reference to a variable can be traversed only after the declaration of that variable,
    // so it is correct to map only references whose descriptors have copies.
    // However such approach can be incorrect when copying functions, classes etc.

    override fun mapValueReference(descriptor: ValueDescriptor) =
            copiedVariables[descriptor] ?: descriptor

    override fun mapVariableReference(descriptor: VariableDescriptor) =
            copiedVariables[descriptor] ?: descriptor
}