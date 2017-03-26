package org.saintandreas.gl;

import org.saintandreas.math.Matrix3f;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Quaternion;
import org.saintandreas.math.Vector2f;
import org.saintandreas.math.Vector3f;

public abstract class AbstractTransformable<T extends AbstractTransformable<T>> implements Transformable<T> {
  Matrix4f transform = new Matrix4f();

  @Override
  public Matrix4f getTransform() {
    return transform;
  }

  @SuppressWarnings("unchecked")
  public T set(Matrix4f m) {
    transform = m;
    return (T) this;
  }

  @Override
  public T identity() {
    return set(new Matrix4f());
  }

  @Override
  public T transpose() {
    return set(getTransform().transpose());
  }

  @Override
  public T translate(float x) {
    return translate(new Vector3f(x, 0, 0));
  }

  @Override
  public T translate(Vector2f vec) {
    return set(getTransform().translate(vec));
  }

  @Override
  public T translate(Vector3f vec) {
    return set(getTransform().translate(vec));
  }

  @Override
  public T rotate(float angle, Vector3f axis) {
    return set(getTransform().rotate(angle, axis));
  }

  @Override
  public T rotate(Quaternion q) {
    return set(getTransform().rotate(q));
  }

  @Override
  public T rotate(Matrix3f m) {
    return set(getTransform().rotate(m));
  }

  @Override
  public T scale(Vector3f vec) {
    return set(getTransform().scale(vec));
  }

  @Override
  public T scale(float f) {
    return set(getTransform().scale(f));
  }

  @Override
  public T multiply(Matrix4f m) {
    return set(getTransform().mult(m));
  }

  @Override
  public T preMultiply(Matrix4f m) {
    return set(m.mult(getTransform()));
  }

  @Override
  public T preTranslate(float x) {
    return preTranslate(new Vector2f(x, 0));
  }

  @Override
  public T preTranslate(Vector2f v) {
    return preMultiply(new Matrix4f().translate(v));
  }

  @Override
  public T preTranslate(Vector3f v) {
    return preMultiply(new Matrix4f().translate(v));
  }

  @Override
  public T preRotate(float angle, Vector3f axis) {
    return preMultiply(new Matrix4f().rotate(angle, axis));
  }

  @Override
  public T preRotate(Quaternion q) {
    return preMultiply(new Matrix4f().rotate(q));
  }

  @Override
  public T preRotate(Matrix3f m) {
    return preMultiply(new Matrix4f().rotate(m));
  }

  @Override
  public T lookat(Vector3f eye, Vector3f center, Vector3f up) {
    return set(Matrix4f.lookat(eye, center, up));
  }

  @Override
  public T orthographic(float left, float right, float bottom, float top, float near, float far) {
    return set(Matrix4f.orthographic(left, right, bottom, top, near, far));
  }

  @Override
  public T perspective(float fovy, float aspect, float zNear, float zFar) {
    return set(Matrix4f.perspective(fovy, aspect, zNear, zFar));
  }

  public Vector3f getTranslation() {
    return getTransform().toTranslationVector();
  }

  public Quaternion getRotation() {
    return getTransform().toRotationQuat();
  }

  public T untranslate() {
    return translate(getTranslation().scale(-1));
  }

  public T unrotate() {
    return set(new Matrix4f().translate(getTranslation()));
  }
}
