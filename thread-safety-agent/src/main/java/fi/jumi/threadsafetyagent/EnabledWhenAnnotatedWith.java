// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.threadsafetyagent;

import fi.jumi.threadsafetyagent.util.DoNotTransformException;
import org.objectweb.asm.*;

import java.util.*;

public class EnabledWhenAnnotatedWith extends ClassAdapter {

    private final List<String> myAnnotationDescs = new ArrayList<String>();
    private final String enablerAnnotationDesc;

    public EnabledWhenAnnotatedWith(String enablerAnnotation, ClassVisitor cv) {
        super(cv);
        this.enablerAnnotationDesc = "L" + enablerAnnotation + ";";
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        myAnnotationDescs.add(desc);
        return super.visitAnnotation(desc, visible);
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        checkIsTransformationEnabled();
        return super.visitField(access, name, desc, signature, value);
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        checkIsTransformationEnabled();
        super.visitInnerClass(name, outerName, innerName, access);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        checkIsTransformationEnabled();
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        checkIsTransformationEnabled();
        super.visitOuterClass(owner, name, desc);
    }

    public void visitEnd() {
        checkIsTransformationEnabled();
        super.visitEnd();
    }

    private void checkIsTransformationEnabled() {
        if (!myAnnotationDescs.contains(enablerAnnotationDesc)) {
            throw new DoNotTransformException();
        }
    }
}
