/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.lir.sparc;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.sparc.SPARC.g0;
import static jdk.vm.ci.sparc.SPARCKind.WORD;
import static jdk.vm.ci.sparc.SPARCKind.XWORD;
import static org.graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;

import org.graalvm.compiler.asm.sparc.SPARCMacroAssembler;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.lir.LIRInstructionClass;
import org.graalvm.compiler.lir.Opcode;
import org.graalvm.compiler.lir.asm.CompilationResultBuilder;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.PlatformKind;

public final class SPARCBitManipulationOp extends SPARCLIRInstruction {
    public static final LIRInstructionClass<SPARCBitManipulationOp> TYPE = LIRInstructionClass.create(SPARCBitManipulationOp.class);

    public enum IntrinsicOpcode {
        IBSR(SizeEstimate.create(13)),
        LBSR(SizeEstimate.create(14)),
        BSF(SizeEstimate.create(4));

        final SizeEstimate size;

        IntrinsicOpcode(SizeEstimate size) {
            this.size = size;
        }
    }

    @Opcode private final IntrinsicOpcode opcode;
    @Def protected AllocatableValue result;
    @Alive({REG}) protected AllocatableValue input;
    @Temp({REG}) protected AllocatableValue scratch;

    public SPARCBitManipulationOp(IntrinsicOpcode opcode, AllocatableValue result, AllocatableValue input, LIRGeneratorTool gen) {
        super(TYPE, opcode.size);
        this.opcode = opcode;
        this.result = result;
        this.input = input;
        scratch = gen.newVariable(LIRKind.combine(input));
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, SPARCMacroAssembler masm) {
        Register dst = asRegister(result, WORD);
        if (isRegister(input)) {
            Register src = asRegister(input);
            switch (opcode) {
                case BSF:
                    PlatformKind tkind = input.getPlatformKind();
                    if (tkind == WORD) {
                        masm.sub(src, 1, dst);
                        masm.andn(dst, src, dst);
                        masm.srl(dst, g0, dst);
                        masm.popc(dst, dst);
                    } else if (tkind == XWORD) {
                        masm.sub(src, 1, dst);
                        masm.andn(dst, src, dst);
                        masm.popc(dst, dst);
                    } else {
                        throw GraalError.shouldNotReachHere("missing: " + tkind);
                    }
                    break;
                case IBSR: {
                    PlatformKind ikind = input.getPlatformKind();
                    assert ikind == WORD;
                    Register tmp = asRegister(scratch);
                    assert !tmp.equals(dst);
                    masm.srl(src, 1, tmp);
                    masm.srl(src, 0, dst);
                    masm.or(dst, tmp, dst);
                    masm.srl(dst, 2, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srl(dst, 4, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srl(dst, 8, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srl(dst, 16, tmp);
                    masm.or(dst, tmp, dst);
                    masm.popc(dst, dst);
                    masm.sub(dst, 1, dst);
                    break;
                }
                case LBSR: {
                    PlatformKind lkind = input.getPlatformKind();
                    assert lkind == XWORD;
                    Register tmp = asRegister(scratch);
                    assert !tmp.equals(dst);
                    masm.srlx(src, 1, tmp);
                    masm.or(src, tmp, dst);
                    masm.srlx(dst, 2, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srlx(dst, 4, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srlx(dst, 8, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srlx(dst, 16, tmp);
                    masm.or(dst, tmp, dst);
                    masm.srlx(dst, 32, tmp);
                    masm.or(dst, tmp, dst);
                    masm.popc(dst, dst);
                    masm.sub(dst, 1, dst); // This is required to fit the given structure.
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();

            }
        } else {
            throw GraalError.shouldNotReachHere();
        }
    }
}
