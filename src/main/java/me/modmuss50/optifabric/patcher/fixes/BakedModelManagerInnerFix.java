package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class BakedModelManagerInnerFix implements ClassFixer {
	private static final String SPRITE_ATLAS_DESC = "Lnet/minecraft/class_7766$class_7767;";
	private static final String BLOCK_ATLAS_FIELD = "field_61871";
	private static final String ITEM_ATLAS_FIELD = "field_64469";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		boolean addedBlockField = ensureField(optifine, minecraft, BLOCK_ATLAS_FIELD);
		boolean addedItemField = ensureField(optifine, minecraft, ITEM_ATLAS_FIELD);

		int initializedBlockConstructors = 0;
		int initializedItemConstructors = 0;
		for (MethodNode method : optifine.methods) {
			if (!"<init>".equals(method.name)) continue;

			if (!hasFieldWrite(method, optifine.name, BLOCK_ATLAS_FIELD, SPRITE_ATLAS_DESC)
					&& initializeAtlasField(optifine, method, BLOCK_ATLAS_FIELD, 0)) {
				initializedBlockConstructors++;
			}

			if (!hasFieldWrite(method, optifine.name, ITEM_ATLAS_FIELD, SPRITE_ATLAS_DESC)
					&& initializeAtlasField(optifine, method, ITEM_ATLAS_FIELD, 1)) {
				initializedItemConstructors++;
			}
		}

		log("field_61871 present=" + !addedBlockField + " added=" + addedBlockField
				+ " initializedConstructors=" + initializedBlockConstructors
				+ "; field_64469 present=" + !addedItemField + " added=" + addedItemField
				+ " initializedConstructors=" + initializedItemConstructors);
	}

	private static boolean ensureField(ClassNode optifine, ClassNode minecraft, String name) {
		FieldNode vanillaField = findField(minecraft, name, SPRITE_ATLAS_DESC);
		if (vanillaField == null) {
			log("vanilla " + name + " missing, cannot restore Fabric renderer shadow");
			return false;
		}

		if (findField(optifine, name, SPRITE_ATLAS_DESC) != null) {
			return false;
		}

		optifine.fields.add(copy(vanillaField));
		return true;
	}

	private static FieldNode findField(ClassNode owner, String name, String desc) {
		for (FieldNode field : owner.fields) {
			if (name.equals(field.name) && desc.equals(field.desc)) {
				return field;
			}
		}

		return null;
	}

	private static FieldNode copy(FieldNode field) {
		return new FieldNode(field.access, field.name, field.desc, field.signature, field.value);
	}

	private static boolean hasFieldWrite(MethodNode method, String owner, String name, String desc) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof FieldInsnNode) {
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				if (fieldInsn.getOpcode() == Opcodes.PUTFIELD && owner.equals(fieldInsn.owner)
						&& name.equals(fieldInsn.name) && desc.equals(fieldInsn.desc)) {
					return true;
				}
			}
		}

		return false;
	}

	private static int findFirstParameterIndex(MethodNode method, String desc) {
		return findParameterIndex(method, desc, 0);
	}

	private static int findParameterIndex(MethodNode method, String desc, int occurrence) {
		int localIndex = 1;
		for (Type type : Type.getArgumentTypes(method.desc)) {
			if (desc.equals(type.getDescriptor()) && occurrence-- == 0) {
				return localIndex;
			}
			localIndex += type.getSize();
		}

		return -1;
	}

	private static boolean initializeAtlasField(ClassNode owner, MethodNode method, String fieldName, int parameterOccurrence) {
		int parameterIndex = findParameterIndex(method, SPRITE_ATLAS_DESC, parameterOccurrence);
		if (parameterIndex < 0) {
			log("constructor " + method.desc + " has no atlas parameter " + parameterOccurrence + " for " + fieldName);
			return false;
		}

		AbstractInsnNode insertAfter = findSuperConstructorCall(method);
		if (insertAfter == null) {
			log("constructor " + method.desc + " has no super call for " + fieldName + " init");
			return false;
		}

		InsnList init = new InsnList();
		init.add(new VarInsnNode(Opcodes.ALOAD, 0));
		init.add(new VarInsnNode(Opcodes.ALOAD, parameterIndex));
		init.add(new FieldInsnNode(Opcodes.PUTFIELD, owner.name, fieldName, SPRITE_ATLAS_DESC));
		method.instructions.insert(insertAfter, init);
		method.maxStack = Math.max(method.maxStack, 2);
		return true;
	}

	private static AbstractInsnNode findSuperConstructorCall(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(methodInsn.name)) {
					return methodInsn;
				}
			}
		}

		return null;
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] BakedModelManagerInnerFix " + message);
		System.err.flush();
	}
}
