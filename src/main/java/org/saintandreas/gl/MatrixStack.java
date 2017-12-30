package org.saintandreas.gl;

import java.util.Stack;

import org.saintandreas.math.Matrix4f;

public class MatrixStack extends AbstractTransformable<MatrixStack> {
  public static final MatrixStack MODELVIEW = new MatrixStack();
  public static final MatrixStack PROJECTION = new MatrixStack();
  Stack<Matrix4f> stack = new Stack<>();

  public int size() {
    return stack.size() + 1;
  }

  public MatrixStack pop() {
    set(stack.pop());
    return this;
  }

  public MatrixStack push() {
    stack.push(getTransform());
    return this;
  }

  public Matrix4f top() {
    return getTransform();
  }
}
