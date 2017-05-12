package agent

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class Agent {
    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer(Transformer())
        }
    }
}

class Transformer : ClassFileTransformer {
    override fun transform(loader: ClassLoader?, className: String?, classBeingRedefined: Class<*>?,
                           protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray?): ByteArray? {
        val cw = ClassWriter(0)
        val ca = Adapter(cw)
        val cr = ClassReader(classfileBuffer)
        cr.accept(ca, 0)
        return cw.toByteArray()
    }
}

class Adapter(val lcv: ClassVisitor) : ClassNode(Opcodes.ASM4) {
    override fun visitEnd() {
        @Suppress("UNCHECKED_CAST")
        for (mn in (methods as List<MethodNode>)) {
            var testDetected = false
            for (ins in mn.instructions) {
                if (ins is MethodInsnNode
                        && ins.owner == "example/CoroutineExampleKt"
                        && ins.opcode == Opcodes.INVOKESTATIC
                        && ins.name == "test") {

                    val il = InsnList()
                    il.add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
                    il.add(LdcInsnNode("Test detected"))
                    il.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                            "println", "(Ljava/lang/String;)V", false))
                    mn.instructions.insertBefore(ins, il)
                    testDetected = true
                }
            }
            if (testDetected) {
                mn.maxStack += 2
            }
        }
        accept(lcv)
    }
}